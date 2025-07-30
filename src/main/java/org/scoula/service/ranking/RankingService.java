package org.scoula.service.ranking;

import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.RankingByTraitGroupDto;

import java.util.List;
import java.util.Map;

public interface RankingService {
    MyRankingDto getMyRanking(Long userId, String recordDate);
    List<PopularStockDto> getTop5Stocks();
    List<RankingByTraitGroupDto> getWeeklyRanking();
    Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking();
}
