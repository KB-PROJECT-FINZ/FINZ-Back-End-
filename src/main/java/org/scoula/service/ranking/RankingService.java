package org.scoula.service.ranking;

import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.RankingByTraitGroupDto;

import java.util.List;
import java.util.Map;

public interface RankingService {
    // (1) 내 수익률 및 전체 순위
    MyRankingDto getMyRanking(Long userId);

    // (2) 전체 인기 종목 Top5 (성향 무관, 전체 거래수 기준)
    List<PopularStockDto> getTop5Stocks();

    // (3) 전체 상위 수익률 TOP 100
    List<RankingByTraitGroupDto> getWeeklyRanking();

    // (4) 성향 그룹별 상위 수익률 TOP 100
    Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking();
}
