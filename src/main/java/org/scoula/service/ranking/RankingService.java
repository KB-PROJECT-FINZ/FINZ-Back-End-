package org.scoula.service.ranking;

import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.Top5StockDto;
import org.scoula.domain.ranking.WeeklyRankingDto;

import java.util.List;
import java.util.Map;

public interface RankingService {
    MyRankingDto getMyRanking(Long userId);
    List<Top5StockDto> getTop5Stocks(String week);
    List<WeeklyRankingDto> getWeeklyRanking(String week);
    Map<String, List<WeeklyRankingDto>> getGroupedWeeklyRanking(String week);
}
