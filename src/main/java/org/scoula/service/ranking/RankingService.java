package org.scoula.service.ranking;

import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.RankingByTraitGroupDto;

import java.util.List;
import java.util.Map;

public interface RankingService {
    MyRankingDto getMyRanking(Long userId, String baseDate);
    List<PopularStockDto> getTop5Stocks(String baseDate);
    List<RankingByTraitGroupDto> getWeeklyRanking(String baseDate);
    Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking(String baseDate);

}
