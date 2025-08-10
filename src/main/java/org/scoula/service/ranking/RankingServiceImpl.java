package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.ranking.*;
import org.scoula.mapper.ranking.RankingMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingMapper rankingMapper;

    // baseDate 검증 및 fallback 처리
    private String getValidBaseDate(String baseDate) {
        if (baseDate != null && !baseDate.isBlank()) {
            return baseDate;
        }

        LocalDate today = LocalDate.now();
        LocalDate thisWeekSunday = today.with(DayOfWeek.SUNDAY);
        LocalDate baseSunday = today.isBefore(thisWeekSunday)
                ? thisWeekSunday.minusWeeks(1)
                : thisWeekSunday;

        if (rankingMapper.existsAssetHistoryByDate(baseSunday) > 0) {
            return baseSunday.toString();
        }

        LocalDate fallbackDate = baseSunday.minusWeeks(1);
        log.warn("데이터 없음 → fallback baseDate: {}", fallbackDate);
        return fallbackDate.toString();
    }

    @Override
    public MyRankingDto getMyRanking(Long userId, String baseDate) {
        String validBaseDate = getValidBaseDate(baseDate);
        Map<String, Object> params = Map.of(
                "userId", userId,
                "baseDate", validBaseDate
        );

        MyRankingDto dto = rankingMapper.selectMyRanking(params);
        if (dto != null) {
            dto.setBaseDate(validBaseDate);
        }
        return dto;
    }

    @Override
    public List<PopularStockDto> getRealTimeOrFallbackPopularStocks() {
        // 1) 오늘
        LocalDate today = LocalDate.now();
        List<PopularStockDto> todayList = rankingMapper.selectRealTimePopularStocks(today.toString());
        if (todayList != null && !todayList.isEmpty()) {
            log.info("인기 종목(오늘) {}건 반환", todayList.size());
            return todayList;
        }

        // 2) 어제
        LocalDate yesterday = today.minusDays(1);
        List<PopularStockDto> yesterdayList = rankingMapper.selectRealTimePopularStocks(yesterday.toString());
        if (yesterdayList != null && !yesterdayList.isEmpty()) {
            log.info("인기 종목(어제) {}건 반환", yesterdayList.size());
            return yesterdayList;
        }

        // 3) 지난주(월~일)
        LocalDate lastWeekMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        log.warn("오늘/어제 데이터 없음 → 지난주({}) 주간 데이터 반환", lastWeekMonday);
        List<PopularStockDto> lastWeekList = rankingMapper.selectPopularStocks(lastWeekMonday.toString());
        return lastWeekList != null ? lastWeekList : List.of();
    }

    @Override
    public List<RankingByTraitGroupDto> getWeeklyRanking(String baseDate) {
        String validBaseDate = getValidBaseDate(baseDate);
        return rankingMapper.selectTopRankingWithTraitGroup(validBaseDate);
    }

    @Override
    public Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking(String baseDate) {
        String validBaseDate = getValidBaseDate(baseDate);

        List<String> groups = List.of("CONSERVATIVE", "BALANCED", "AGGRESSIVE", "ANALYTICAL", "EMOTIONAL");
        Map<String, List<RankingByTraitGroupDto>> result = new LinkedHashMap<>();

        Map<String, String> traitMap = Map.ofEntries(
                Map.entry("CAG", "보수형"), Map.entry("CSD", "보수형"), Map.entry("IND", "보수형"), Map.entry("VAL", "보수형"),
                Map.entry("BGT", "균형형"), Map.entry("BSS", "균형형"), Map.entry("AID", "균형형"),
                Map.entry("AGR", "공격형"), Map.entry("DTA", "공격형"), Map.entry("EXP", "공격형"), Map.entry("THE", "공격형"),
                Map.entry("INF", "특수형"), Map.entry("SYS", "특수형"), Map.entry("TEC", "특수형"), Map.entry("SOC", "특수형")
        );

        for (String group : groups) {
            List<RankingByTraitGroupDto> list = rankingMapper.selectTopRankingByTraitGroup(group, validBaseDate);
            for (RankingByTraitGroupDto dto : list) {
                dto.setTraitGroup(traitMap.getOrDefault(dto.getOriginalTrait(), "기타"));
            }
            result.put(group, list);
        }

        return result;
    }
}
