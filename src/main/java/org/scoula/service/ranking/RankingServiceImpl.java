package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.*;
import org.scoula.mapper.ranking.RankingMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingMapper rankingMapper;

    @Override
    public MyRankingDto getMyRanking(Long userId) {
        return rankingMapper.selectMyRanking(userId);
    }

    @Override
    public List<PopularStockDto> getPopularStocks(String dateType, Date baseDate) {
        return rankingMapper.selectPopularStocks(dateType, baseDate);
    }

    @Override
    public Map<String, List<TraitGroupRankingDto>> getTraitGroupRanking(String dateType, Date baseDate) {
        List<TraitGroupRankingDto> raw = rankingMapper.selectTraitGroupRanking(dateType, baseDate);
        return raw.stream()
                .collect(Collectors.groupingBy(TraitGroupRankingDto::getTraitGroup));
    }
}



