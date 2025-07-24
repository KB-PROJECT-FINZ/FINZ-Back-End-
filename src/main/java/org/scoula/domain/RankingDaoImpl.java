package org.scoula.domain;

import org.scoula.mapper.RankingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RankingDaoImpl implements RankingDao {

    @Autowired
    private RankingMapper rankingMapper;

    @Override
    public MyRankingDto getMyRanking(Long userId) {
        return rankingMapper.selectMyRanking(userId);
    }

    @Override
    public List<Top5StockDto> getTop5Stocks(String week, String traitType) {
        return rankingMapper.selectTop5Stocks(week,traitType);
    }

    @Override
    public List<WeeklyRankingDto> getWeeklyRanking(String week) {
        return rankingMapper.selectWeeklyRanking(week);
    }

    @Override
    public List<WeeklyRankingWithGroupDto> getGroupedWeeklyRanking(String week) {
        return rankingMapper.selectGroupedWeeklyRanking(week);
    }
}
