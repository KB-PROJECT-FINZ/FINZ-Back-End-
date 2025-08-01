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
    List<TraitStockDto> findTraitStocks(@Param("userId") Long userId);

    List<MyDistributionDto> findMyDistribution(@Param("userId") Long userId);

    List<PopularStockDto> findPopularStocksByTrait(@Param("traitGroup") String traitGroup);
}
