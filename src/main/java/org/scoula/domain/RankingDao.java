package org.scoula.domain;

import java.util.List;

public interface RankingDao {

    MyRankingDto getMyRanking(Long userId);
    List<Top5StockDto> getTop5Stocks(String week,String traitType);
    List<WeeklyRankingDto> getWeeklyRanking(String week);
    List<WeeklyRankingWithGroupDto> getGroupedWeeklyRanking(String week);
}
