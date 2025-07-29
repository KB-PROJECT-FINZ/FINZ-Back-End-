package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.*;

import java.util.List;

@Mapper
public interface RankingMapper {
    // 내 수익률 및 순위
    MyRankingDto selectMyRanking(@Param("userId") Long userId);

    MyRankingDto selectMyRanking(@Param("userId") Long userId, @Param("dateType") String dateType, @Param("baseDate") String baseDate);
    List<PopularStockDto> selectPopularStocks(@Param("dateType") String dateType, @Param("baseDate") String baseDate);
    List<MyRankingDto> selectTopRanking(@Param("dateType") String dateType, @Param("baseDate") String baseDate);
    List<RankingByTraitGroupDto> selectTopRankingByTraitGroup(@Param("traitGroup") String traitGroup, @Param("dateType") String dateType, @Param("baseDate") String baseDate);
}


