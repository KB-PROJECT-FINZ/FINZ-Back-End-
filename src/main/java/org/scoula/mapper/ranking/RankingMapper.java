package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.*;

import java.util.List;

@Mapper
public interface RankingMapper {
    MyRankingDto selectMyRanking(@Param("userId") Long userId, @Param("baseDate") String baseDate);
    List<PopularStockDto> selectPopularStocks(@Param("baseDate") String baseDate);

    List<MyRankingDto> selectTopRanking(@Param("baseDate") String baseDate);

    List<RankingByTraitGroupDto> selectTopRankingByTraitGroup(
            @Param("traitGroup") String traitGroup,
            @Param("baseDate") String baseDate
    );
}


