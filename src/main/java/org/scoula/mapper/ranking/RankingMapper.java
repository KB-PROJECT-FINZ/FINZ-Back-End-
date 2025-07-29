package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.*;

import java.util.List;

@Mapper
public interface RankingMapper {
    MyRankingDto selectMyRanking(@Param("userId") Long userId);

    List<PopularStockDto> selectPopularStocks(@Param("dateType") String dateType,
                                              @Param("baseDate") Date baseDate);

    List<TraitGroupRankingDto> selectTraitGroupRanking(@Param("dateType") String dateType,
                                                       @Param("baseDate") Date baseDate);
}

