package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.*;
import org.scoula.mapper.ranking.RankingMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingMapper rankingMapper;

    // baseDate 검증 및 fallback 처리
    private String getValidBaseDate(String baseDate) {
        System.out.println("📌 [getValidBaseDate] baseDate 파라미터: " + baseDate);

        if (baseDate != null && !baseDate.isBlank()) {
            return baseDate;
        }

        LocalDate today = LocalDate.now();
        LocalDate thisWeekSunday = today.with(DayOfWeek.SUNDAY);
        LocalDate baseSunday = today.isBefore(thisWeekSunday) ? thisWeekSunday.minusWeeks(1) : thisWeekSunday;

        System.out.println("🔍 오늘 날짜 기준 baseSunday: " + baseSunday);

        int count = rankingMapper.existsAssetHistoryByDate(baseSunday); // 여기를 int로!
        boolean hasData = count > 0;

        if (hasData) {
            System.out.println("✅ 해당 날짜에 데이터 있음 → " + baseSunday);
            return baseSunday.toString();
        }

        LocalDate fallbackDate = baseSunday.minusWeeks(1);
        System.out.println("⚠️ 데이터 없음, fallback baseDate: " + fallbackDate);
        return fallbackDate.toString();
    }


    @Override
    public MyRankingDto getMyRanking(Long userId, String baseDate) {
        // baseDate가 null이면 유효한 주간 시작일로 대체
        String validBaseDate = getValidBaseDate(baseDate);

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("baseDate", validBaseDate);

        MyRankingDto dto = rankingMapper.selectMyRanking(params);
        if (dto != null) {
            dto.setBaseDate(validBaseDate);  // baseDate를 DTO에 명확히 세팅
        }
        return dto;
    }

    @Override
    public List<PopularStockDto> getRealTimeOrFallbackPopularStocks() {
        List<PopularStockDto> realTimeStocks = rankingMapper.selectRealTimePopularStocks(LocalDate.now().toString());

        if (realTimeStocks == null || realTimeStocks.isEmpty()) {
            LocalDate lastWeekBase = LocalDate.now().with(DayOfWeek.SUNDAY).minusWeeks(1);
            realTimeStocks = rankingMapper.selectPopularStocks(lastWeekBase.toString());
        }

        return realTimeStocks;
    }

    @Override
    public List<RankingByTraitGroupDto> getWeeklyRanking(String baseDate) {
        String validBaseDate = getValidBaseDate(baseDate);
        // 전체 랭킹 조회 (성향 그룹 조건 없음)
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
                String original = dto.getOriginalTrait();
                String traitKr = traitMap.getOrDefault(original, "기타");
                dto.setTraitGroup(traitKr);
            }
            result.put(group, list); // group 은 "CONSERVATIVE" 등이므로 key 명확히 변경하려면 여기서 변환
        }

        return result;
    }
}