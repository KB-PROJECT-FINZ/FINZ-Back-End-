package org.scoula.service.ranking;

import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.PopularStockDto;

import java.util.List;
import java.util.Map;

public interface RankingService {
    // 내 수익률, 순위, 상위 퍼센트
    MyRankingDto getMyRanking(Long userId);

    // 전체 인기 종목 Top5 (매수+매도 포함, 성향 무관)
    List<PopularStockDto> getPopularStocks(String dateType, Date baseDate);

    // 성향 그룹별 수익률 랭킹
    Map<String, List<TraitGroupRankingDto>> getTraitGroupRanking(String dateType, Date baseDate);
}
