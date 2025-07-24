package org.scoula.service;

import org.scoula.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RankingServiceImpl implements RankingService{

    @Autowired
    private RankingDao rankingDao;

    //내수익률 및 순위
    @Override
    public MyRankingDto getMyRanking(Long userId) {
        return rankingDao.getMyRanking(userId);
    }

    //인기 종목 Top5
    @Override
    public List<Top5StockDto> getTop5Stocks(String week, String traitType) {
        return rankingDao.getTop5Stocks(week,traitType);
    }
    //주간 랭킹
    @Override
    public List<WeeklyRankingDto> getWeeklyRanking(String week) {
        return rankingDao.getWeeklyRanking(week);
    }

    //주간 성향별 랭킹
    @Override
    public Map<String, List<WeeklyRankingDto>> getGroupedWeeklyRanking(String week) {
        List<WeeklyRankingWithGroupDto> rawList = rankingDao.getGroupedWeeklyRanking(week);
        return rawList.stream()
                .collect(Collectors.groupingBy(
                        WeeklyRankingWithGroupDto::getTraitGroup,
                        Collectors.mapping(dto -> {
                            WeeklyRankingDto simpleDto = new WeeklyRankingDto();
                            simpleDto.setUserId(dto.getUserId());
                            simpleDto.setName(dto.getName());
                            simpleDto.setGainRate(dto.getGainRate());
                            simpleDto.setRanking(dto.getRanking());
                            return simpleDto;
                        }, Collectors.toList())
                ));
    }






}
