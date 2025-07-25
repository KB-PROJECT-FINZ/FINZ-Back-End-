package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.scoula.domain.ranking.MyDistributionDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;

import java.util.List;

@Mapper
public interface AnalysisMapper {
    // 성향별 보유 비중 데이터 조회 쿼리 매핑
    List<TraitStockDto> findTraitStocks();

    // 내 수익률 분포 위치 데이터 조회 쿼리 매핑
    List<MyDistributionDto> findMyDistribution(Long userId);

    // 유사 성향 투자자 인기 종목 조회 쿼리 매핑
    List<PopularStockDto> findPopularStocksByTrait(String trait);
}
