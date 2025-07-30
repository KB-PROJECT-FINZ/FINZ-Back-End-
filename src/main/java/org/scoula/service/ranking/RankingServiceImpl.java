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

    private String getThisWeekMondayDate() {
        LocalDate today = LocalDate.now();
        // 이번 주 월요일 계산
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        return monday.toString(); // 'YYYY-MM-DD' 형식
    }
    @Override
    public MyRankingDto getMyRanking(Long userId, String recordDate) {
        if (recordDate == null || recordDate.isEmpty()) {
            recordDate = getThisWeekMondayDate();
        }
        return rankingMapper.selectMyRanking(userId, recordDate);
    }

    @Override
    public List<PopularStockDto> getTop5Stocks() {
        String recordDate = getThisWeekMondayDate();
        return rankingMapper.selectPopularStocks(recordDate);
    }

    @Override
    public List<RankingByTraitGroupDto> getWeeklyRanking() {
        String recordDate = getThisWeekMondayDate();
        List<MyRankingDto> top100 = rankingMapper.selectTopRanking(recordDate);
        return top100.stream()
                .map(dto -> new RankingByTraitGroupDto(dto.getUserId(), "UNKNOWN", dto.getGainRate(), dto.getRanking()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking() {
        String recordDate = getThisWeekMondayDate();
        List<String> groups = List.of("CONSERVATIVE", "BALANCED", "AGGRESSIVE", "SPECIAL");

        Map<String, List<RankingByTraitGroupDto>> result = new HashMap<>();
        for (String group : groups) {
            List<RankingByTraitGroupDto> list = rankingMapper.selectTopRankingByTraitGroup(group, recordDate);
            result.put(group, list);
        }
        return result;
    }
}