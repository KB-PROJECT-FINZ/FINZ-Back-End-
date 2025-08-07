package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface RankingMapper {
    //실시간 인기 종목
    List<PopularStockDto> selectRealTimePopularStocks(@Param("baseDate") String baseDate);
    //주간인기종목
    List<PopularStockDto> selectPopularStocks(@Param("baseDate") String baseDate);
    //특정 날짜에  자산이력 조회
    int existsAssetHistoryByDate(@Param("baseDate") LocalDate baseDate);
    //사용자의 주간 수익률
    MyRankingDto selectMyRanking(Map<String, Object> params);
    //전체 사용자 주간 랭킹
    List<RankingByTraitGroupDto> selectTopRankingWithTraitGroup(@Param("baseDate") String baseDate);
    // 성향별 랭킹
    List<RankingByTraitGroupDto> selectTopRankingByTraitGroup(@Param("traitGroup") String traitGroup, @Param("baseDate") String baseDate);



}