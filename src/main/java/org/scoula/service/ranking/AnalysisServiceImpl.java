package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.MyDistributionDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;
import org.scoula.mapper.ranking.AnalysisMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {


    private final AnalysisMapper analysisMapper;

    @Override
    public List<TraitStockDto> getTraitStocks(Long userId) {
        return analysisMapper.findTraitStocks(userId);
    }

    @Override
    public List<MyDistributionDto> getMyDistribution(Long userId) {
        return analysisMapper.findMyDistribution(userId);
    }

    @Override
    public List<PopularStockDto> getPopularStocksByTrait(String traitGroup) {
        String normalizedGroup = traitGroup;
        if ("ANALYTICAL".equalsIgnoreCase(traitGroup) || "EMOTIONAL".equalsIgnoreCase(traitGroup)) {
            normalizedGroup = "SPECIAL";
        }
        return analysisMapper.findPopularStocksByTrait(normalizedGroup);
    }
}