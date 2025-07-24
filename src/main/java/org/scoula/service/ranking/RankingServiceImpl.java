package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.Top5StockDto;
import org.scoula.domain.ranking.WeeklyRankingDto;
import org.scoula.domain.ranking.WeeklyRankingWithGroupDto;
import org.scoula.mapper.ranking.RankingMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingMapper rankingMapper;


    //내수익률 및 순위
    @Override
    public MyRankingDto getMyRanking(Long userId) {
        return rankingMapper.selectMyRanking(userId);
    }

    //인기 종목 Top5
    @Override
    public List<Top5StockDto> getTop5Stocks(String week) {
        return rankingMapper.selectTop5Stocks(week);
    }
    //주간 랭킹
    @Override
    public List<WeeklyRankingDto> getWeeklyRanking(String week) {
        return rankingMapper.selectWeeklyRanking(week);
    }

    //주간 성향별 랭킹
    @Override
    public Map<String, List<WeeklyRankingDto>> getGroupedWeeklyRanking(String week) {
        List<WeeklyRankingWithGroupDto> rawList = rankingMapper.selectGroupedWeeklyRanking(week);
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



