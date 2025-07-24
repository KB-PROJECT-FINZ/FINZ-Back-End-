package org.scoula.service;

import org.scoula.domain.MyRankingDto;
import org.scoula.domain.Top5StockDto;
import org.scoula.domain.WeeklyRankingDto;

import java.util.List;
import java.util.Map;

public interface RankingService {
    MyRankingDto getMyRanking(Long userId);
    List<Top5StockDto> getTop5Stocks(String week, String traitType);
    List<WeeklyRankingDto> getWeeklyRanking(String week);
    Map<String, List<WeeklyRankingDto>> getGroupedWeeklyRanking(String week);
}
