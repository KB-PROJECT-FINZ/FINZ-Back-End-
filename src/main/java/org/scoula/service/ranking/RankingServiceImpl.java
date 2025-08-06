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
        if (baseDate != null && !baseDate.isBlank()) {
            return baseDate;
        }

        LocalDate today = LocalDate.now();
        LocalDate thisWeekBase = today.with(DayOfWeek.SUNDAY);
        LocalDate lastWeekBase = thisWeekBase.minusWeeks(1);

        boolean hasData = rankingMapper.existsAssetHistoryByDate(thisWeekBase);
        return hasData ? thisWeekBase.toString() : lastWeekBase.toString();
    }

    @Override
    public MyRankingDto getMyRanking(Long userId, String baseDate) {
        String validBaseDate = getValidBaseDate(baseDate);
        MyRankingDto dto = rankingMapper.selectMyRanking(userId, validBaseDate);
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
            for (RankingByTraitGroupDto dto : list) {
                String key = dto.getTraitGroup();
                result.computeIfAbsent(key, k -> new ArrayList<>()).add(dto);
            }
        }

        return result;
    }

}