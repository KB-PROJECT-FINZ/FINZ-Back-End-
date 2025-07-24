package org.scoula.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.domain.MyRankingDto;
import org.scoula.domain.Top5StockDto;
import org.scoula.domain.WeeklyRankingDto;
import org.scoula.domain.WeeklyRankingWithGroupDto;

import java.util.List;

public interface RankingMapper {
    MyRankingDto selectMyRanking(@Param("userId") Long userId);
    List<Top5StockDto> selectTop5Stocks(@Param("week") String week, @Param("traitType") String traitType);
    List<WeeklyRankingDto> selectWeeklyRanking(@Param("week") String week);
    List<WeeklyRankingWithGroupDto> selectGroupedWeeklyRanking(@Param("week") String week);
}
