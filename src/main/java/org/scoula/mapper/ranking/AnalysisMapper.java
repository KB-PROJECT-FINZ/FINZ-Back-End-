package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.MyDistributionDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {
    // 성향별 보유 비중 조회
    List<TraitStockDto> findTraitStocks(@Param("userId") Long userId);

    // 내 수익률 분포 조회
    List<MyDistributionDto> findMyDistribution(@Param("userId") Long userId);

    // 유사 성향 투자자 인기 종목 조회
    List<PopularStockDto> findPopularStocksByTrait(@Param("traitGroup") String traitGroup);

    // 내 수익률 분포 업데이트 (stockCode 기준)
    int updateDistribution(@Param("userId") int userId,
                           @Param("distribution") MyDistributionDto distribution);

    // 내 수익률 분포 신규 저장
    void insertDistribution(@Param("userId") int userId, @Param("distribution") MyDistributionDto distribution);

}
