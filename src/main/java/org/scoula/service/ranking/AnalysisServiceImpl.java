package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.MyDistributionDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;
import org.scoula.mapper.ranking.AnalysisMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisMapper analysisMapper;

    // 성향별 보유 비중 분석 데이터 반환
    @Override
    public List<TraitStockDto> getTraitStocks() {
        return analysisMapper.findTraitStocks();
    }

    // 내 수익률 분포 위치 데이터 반환
    @Override
    public List<MyDistributionDto> getMyDistribution(Long userId) {
        return analysisMapper.findMyDistribution(userId);
    }

    // 유사 성향 투자자 인기 종목 반환
    @Override
    public List<PopularStockDto> getPopularStocksByTrait(String trait) {
        return analysisMapper.findPopularStocksByTrait(trait);
    }
}
