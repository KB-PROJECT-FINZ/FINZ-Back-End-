package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.Top5StockDto;
import org.scoula.domain.ranking.WeeklyRankingDto;
import org.scoula.domain.ranking.WeeklyRankingWithGroupDto;

import java.util.List;

@Mapper
public interface RankingMapper {
    MyRankingDto selectMyRanking(@Param("userId") Long userId);
    List<Top5StockDto> selectTop5Stocks(@Param("week") String week);
    List<WeeklyRankingDto> selectWeeklyRanking(@Param("week") String week);
    List<WeeklyRankingWithGroupDto> selectGroupedWeeklyRanking(@Param("week") String week);
}
