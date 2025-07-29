package org.scoula.service.ranking;

import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;

import java.util.List;

public interface AnalysisService {
    // 성향별 보유 비중 분석 데이터 조회
    List<TraitStockDto> getTraitStocks();

    // 내 수익률 분포 위치 데이터 조회
    List<MyDistributionDto> getMyDistribution(Long userId);

    // 유사 성향 투자자 인기 종목 조회
    List<PopularStockDto> getPopularStocksByTrait(String trait);
}
