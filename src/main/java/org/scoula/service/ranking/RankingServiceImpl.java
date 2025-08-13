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

    // ✅ baseDate 파라미터가 오면 그대로 쓰되, 없으면 "가장 최근 주간 캐시"를 자동 선택
    private String resolveLatestWeekBaseDate(String baseDate) {
        if (baseDate != null && !baseDate.isBlank()) {
            // 전달된 baseDate에도 캐시가 있는지 확인
            if (rankingMapper.existsWeekCacheByDate(baseDate) == 1) return baseDate;
            log.warn("요청 baseDate={} 에 대한 주간 캐시가 없습니다. 최신 캐시로 대체합니다.", baseDate);
        }
        String latest = rankingMapper.selectLatestWeekBaseDate();
        if (latest == null) {
            log.error("주간 캐시가 하나도 없습니다. 배치 적재를 먼저 수행하세요.");
            // 비상시: 지난주 일요일로 시도(없으면 빈값 반환될 수 있음)
            LocalDate fallback = LocalDate.now().with(DayOfWeek.SUNDAY).minusWeeks(1);
            return fallback.toString();
        }
        return latest;
    }

    @Override
    public MyRankingDto getMyRanking(Long userId, String baseDate) {
        String bd = resolveLatestWeekBaseDate(baseDate);
        MyRankingDto dto = rankingMapper.selectMyRankingCached(userId, bd);
        if (dto != null) dto.setBaseDate(bd);
        return dto; // dto가 null이면 프론트에서 "데이터 없음" 처리
    }

    @Override
    public List<RankingByTraitGroupDto> getWeeklyRanking(String baseDate) {
        String bd = resolveLatestWeekBaseDate(baseDate);
        return rankingMapper.selectWeeklyRankingCached(bd);
    }

    @Override
    public Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking(String baseDate) {
        String bd = resolveLatestWeekBaseDate(baseDate);
        Map<String, List<RankingByTraitGroupDto>> res = new LinkedHashMap<>();
        for (String g : List.of("CONSERVATIVE","BALANCED","AGGRESSIVE","ANALYTICAL","EMOTIONAL")) {
            res.put(g, rankingMapper.selectGroupedWeeklyRankingCached(bd, g));
        }
        return res;
    }

    // "오늘 DAY 없으면 지난주 WEEK"는 그대로 유지
    @Override
    public List<PopularStockDto> getRealTimeOrFallbackPopularStocks() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        String today = LocalDate.now(KST).toString();
        String yesterday = LocalDate.now(KST).minusDays(1).toString();

        // 1) 오늘 DAY
        List<PopularStockDto> day = rankingMapper.selectPopularStocksCachedDay(today);
        if (day != null && !day.isEmpty()) return day;

        // 2) 어제 DAY
        List<PopularStockDto> dayPrev = rankingMapper.selectPopularStocksCachedDay(yesterday);
        if (dayPrev != null && !dayPrev.isEmpty()) return dayPrev;

        // 3) ✅ popular_stocks에서 최신 WEEK 기준일로 폴백
        String latestWeek = rankingMapper.selectLatestWeekBaseDateFromPopular();
        if (latestWeek != null) {
            List<PopularStockDto> week = rankingMapper.selectPopularStocksCachedWeek(latestWeek);
            if (week != null && !week.isEmpty()) return week;
        }

        // 4) 정말 없으면 빈 배열
        return List.of();
    }
}