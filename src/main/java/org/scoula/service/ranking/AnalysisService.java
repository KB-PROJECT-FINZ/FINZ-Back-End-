package org.scoula.service.ranking;

import org.scoula.domain.ranking.MyDistributionDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;

import java.util.List;

public interface AnalysisService {
    // (1) 성향별 보유 비중 분석
    List<TraitStockDto> getTraitStocks(Long userId);

    // (2) 내 수익률 분포 위치
    List<MyDistributionDto> getMyDistribution(Long userId);

    // (3) 유사 성향 투자자 인기 종목 조회
    List<PopularStockDto> getPopularStocksByTrait(String traitGroup);

    void saveMyStockDistribution(Long userId, List<MyDistributionDto> distributions);





}
