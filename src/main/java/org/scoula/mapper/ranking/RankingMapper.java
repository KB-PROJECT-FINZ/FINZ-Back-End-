package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface RankingMapper {

    // 최신 base_date (WEEK)
    String selectLatestWeekBaseDate();

    // ✅ 캐시 존재 여부 (주차)
    int existsWeekCacheByDate(@Param("baseDate") String baseDate);

    // 인기 종목 (캐시)
    List<PopularStockDto> selectPopularStocksCachedDay(@Param("baseDate") String baseDate);
    List<PopularStockDto> selectPopularStocksCachedWeek(@Param("baseDate") String baseDate);

    // ✅ popular_stocks에서 최신 WEEK base_date 찾기
    String selectLatestWeekBaseDateFromPopular();

    // 주간 랭킹 (캐시)
    List<RankingByTraitGroupDto> selectWeeklyRankingCached(@Param("baseDate") String baseDate);
    List<RankingByTraitGroupDto> selectGroupedWeeklyRankingCached(@Param("baseDate") String baseDate,
                                                                  @Param("traitGroup") String traitGroup);

    // 내 랭킹 (캐시)
    MyRankingDto selectMyRankingCached(@Param("userId") Long userId,
                                       @Param("baseDate") String baseDate);

    // 🔽 필요 없으면 제거
    // int existsRankingCacheByDate(@Param("baseDate") java.time.LocalDate baseDate);

    // 최신 주차를 anchor 이하(<=)에서만 찾는 쿼리
    String selectLatestWeekBaseDateLTE(@Param("anchor") String anchor);
}