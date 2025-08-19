package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.ranking.*;
import org.scoula.mapper.ranking.RankingMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingMapper rankingMapper;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 오늘 기준 '지난주 일요일' (일~토 완결 주 anchor) */
    private String lastSundayKST() {
        LocalDate today = LocalDate.now(KST);
        int dow = today.getDayOfWeek().getValue() % 7; // SUN=0
        LocalDate lastSunday = today.minusDays(dow + 7);
        return lastSunday.toString(); // YYYY-MM-DD
    }

    /** 임의 날짜를 그 주의 '일요일'로 정규화 */
    private String normalizeToSunday(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.isBlank()) return null;
        LocalDate d = LocalDate.parse(yyyyMmDd);
        int dow = d.getDayOfWeek().getValue() % 7; // SUN=0
        return d.minusDays(dow).toString();
    }

    /**
     * anchor 주차에 캐시가 없고, 사용자가 주를 지정하지 않은 경우에만
     * anchor 이하(<=)에서 가장 최신 주로 1회 폴백
     */
    private String maybeFallbackToLatestLTE(String anchor, boolean userSpecified) {
        if (userSpecified) return anchor; // 사용자가 명시했으면 다운그레이드 금지
        Integer exists = rankingMapper.existsWeekCacheByDate(anchor);
        if (exists != null && exists == 1) return anchor;
        String latestLTE = rankingMapper.selectLatestWeekBaseDateLTE(anchor);
        if (latestLTE != null) {
            log.warn("anchor={} 주차 캐시 없음 → 과거 최신 주({})로 폴백", anchor, latestLTE);
            return latestLTE;
        }
        log.warn("anchor={} 주차 및 과거 데이터 모두 없음", anchor);
        return anchor; // 어차피 쿼리 결과는 빈값이 됨
    }

    /**
     * 요청 baseDate 처리:
     *  - 들어오면: 그 주의 일요일로 정규화 (userSpecified=true)
     *  - 없으면: 지난주 일요일(anchor) 계산 후, 캐시 없으면 anchor 이하에서 최신 주로 1회 폴백
     */
    private String resolveQueryBaseDate(String baseDate) {
        boolean userSpecified = (baseDate != null && !baseDate.isBlank());
        String anchor = userSpecified ? normalizeToSunday(baseDate) : lastSundayKST();
        return maybeFallbackToLatestLTE(anchor, userSpecified);
    }

    // =========================
    // 서비스 구현
    // =========================

    /** 내 주간 랭킹 */
    @Override
    public MyRankingDto getMyRanking(Long userId, String baseDate) {
        String qDate = resolveQueryBaseDate(baseDate);
        MyRankingDto dto = rankingMapper.selectMyRankingCached(userId, qDate);
        if (dto != null) dto.setBaseDate(qDate);
        log.debug("getMyRanking userId={}, qDate={}, exists={}", userId, qDate, dto != null);
        return dto; // null이면 프론트에서 '데이터 없음' 처리
    }

    /** 오늘 인기 종목 Top10 (DAY → 없으면 어제 DAY → 없으면 지난주 WEEK 폴백) */
    @Override
    public List<PopularStockDto> getRealTimeOrFallbackPopularStocks() {
        String today = LocalDate.now(KST).toString();
        String yesterday = LocalDate.now(KST).minusDays(1).toString();

        // 1) 오늘 DAY
        List<PopularStockDto> day = rankingMapper.selectPopularStocksCachedDay(today);
        if (day != null && !day.isEmpty()) {
            log.debug("popular-stocks: using DAY(today={})", today);
            return day;
        }

        // 2) 어제 DAY  ✅ 버그 수정: dayPrev 를 검사해야 함
        List<PopularStockDto> dayPrev = rankingMapper.selectPopularStocksCachedDay(yesterday);
        if (dayPrev != null && !dayPrev.isEmpty()) {
            log.debug("popular-stocks: using DAY(yesterday={})", yesterday);
            return dayPrev;
        }

        // 3) 지난주 WEEK (anchor=지난주 일요일)
        String weekAnchor = lastSundayKST();
        List<PopularStockDto> week = rankingMapper.selectPopularStocksCachedWeek(weekAnchor);
        if (week != null && !week.isEmpty()) {
            log.debug("popular-stocks: fallback WEEK(anchor={})", weekAnchor);
            return week;
        }

        log.warn("popular-stocks: no data for today/yesterday/week fallback");
        return List.of();
    }

    /** 주간 전체 랭킹: baseDate 없으면 지난주 anchor → 없으면 anchor 이하 최신으로 폴백 */
    @Override
    public List<RankingByTraitGroupDto> getWeeklyRanking(String baseDate) {
        String qDate = resolveQueryBaseDate(baseDate);
        log.debug("getWeeklyRanking qDate={}", qDate);
        return rankingMapper.selectWeeklyRankingCached(qDate);
    }

    /** 주간 성향별 랭킹: 동일 규칙 */
    @Override
    public Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking(String baseDate) {
        String qDate = resolveQueryBaseDate(baseDate);
        log.debug("getGroupedWeeklyRanking qDate={}", qDate);

        Map<String, List<RankingByTraitGroupDto>> res = new LinkedHashMap<>();
        for (String g : List.of("CONSERVATIVE", "BALANCED", "AGGRESSIVE", "ANALYTICAL", "EMOTIONAL")) {
            res.put(g, rankingMapper.selectGroupedWeeklyRankingCached(qDate, g));
        }
        return res;
    }
}