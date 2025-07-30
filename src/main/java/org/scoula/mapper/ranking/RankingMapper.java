package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.*;

import java.util.List;

@Mapper
public interface RankingMapper {
    MyRankingDto selectMyRanking(@Param("userId") Long userId,
                                 @Param("recordDate") String recordDate);

    List<PopularStockDto> selectPopularStocks(@Param("recordDate") String recordDate);

    List<MyRankingDto> selectTopRanking(@Param("recordDate") String recordDate);

    List<RankingByTraitGroupDto> selectTopRankingByTraitGroup(@Param("traitGroup") String traitGroup,
                                                              @Param("recordDate") String recordDate);
}


