package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.*;
import org.scoula.mapper.ranking.RankingMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingMapper rankingMapper;

    private String getLastWeekMondayDate() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        return lastMonday.toString(); // 'YYYY-MM-DD' 형태 문자열 반환
    }

    @Override
    public MyRankingDto getMyRanking(Long userId, String baseDate) {
        if (baseDate == null || baseDate.isBlank()) {
            baseDate = getLastWeekMondayDate();
        }
        return rankingMapper.selectMyRanking(userId, baseDate);
    }

    @Override
    public List<PopularStockDto> getTop5Stocks(String baseDate) {
        if (baseDate == null || baseDate.isBlank()) {
            baseDate = getLastWeekMondayDate();
        }
        return rankingMapper.selectPopularStocks(baseDate);
    }

    @Override
    public List<RankingByTraitGroupDto> getWeeklyRanking(String baseDate) {
        if (baseDate == null || baseDate.isBlank()) {
            baseDate = getLastWeekMondayDate();
        }
        return rankingMapper.selectTopRankingWithTraitGroup(baseDate);
    }

    @Override
    public Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking(String baseDate) {
        System.out.println("!!getGroupedWeeklyRanking called with baseDate=" + baseDate);
        if (baseDate == null || baseDate.isBlank()) {
            baseDate = getLastWeekMondayDate();
        }
        System.out.println("Effective baseDate=" + baseDate);

        List<String> groups = List.of("CONSERVATIVE", "BALANCED", "AGGRESSIVE", "ANALYTICAL", "EMOTIONAL");
        Map<String, List<RankingByTraitGroupDto>> result = new LinkedHashMap<>();

        Map<String, String> traitMap = Map.ofEntries(
                Map.entry("CAG", "보수형"),
                Map.entry("CSD", "보수형"),
                Map.entry("IND", "보수형"),
                Map.entry("VAL", "보수형"),
                Map.entry("BGT", "균형형"),
                Map.entry("BSS", "균형형"),
                Map.entry("AID", "균형형"),
                Map.entry("AGR", "공격형"),
                Map.entry("DTA", "공격형"),
                Map.entry("EXP", "공격형"),
                Map.entry("THE", "공격형"),
                Map.entry("INF", "특수형"),
                Map.entry("SYS", "특수형"),
                Map.entry("TEC", "특수형"),
                Map.entry("SOC", "특수형")
        );

        for (String group : groups) {
            System.out.println("Processing group: " + group);
            List<RankingByTraitGroupDto> list = rankingMapper.selectTopRankingByTraitGroup(group, baseDate);

            System.out.println("Fetched " + list.size() + " records for group: " + group);

            for (RankingByTraitGroupDto dto : list) {
                System.out.println("originalTrait: " + dto.getOriginalTrait());
                String original = dto.getOriginalTrait();
                String traitKr = traitMap.getOrDefault(original, "기타");
                dto.setTraitGroup(traitKr);
            }

            for (RankingByTraitGroupDto dto : list) {
                String key = dto.getTraitGroup();
                result.computeIfAbsent(key, k -> new ArrayList<>()).add(dto);
            }
        }
        System.out.println("Returning grouped ranking result");
        return result;
    }
}