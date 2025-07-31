package org.scoula.controller.ranking;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.MyDistributionDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;
import org.scoula.service.ranking.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ranking/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // (1) 성향별 보유 비중 조회
    @GetMapping("/trait-stock")
    public List<TraitStockDto> getTraitStocks(@RequestParam Long userId) {
        return analysisService.getTraitStocks(userId);
    }

    // (2) 내 수익률 분포 위치 조회
    @GetMapping("/my-distribution")
    public List<MyDistributionDto> getMyDistribution(@RequestParam Long userId) {
        return analysisService.getMyDistribution(userId);
    }

    // (3) 유사 성향 투자자 인기 종목 조회
    @GetMapping("/popular-stocks")
    public List<PopularStockDto> getPopularStocks(@RequestParam String traitGroup) {
        return analysisService.getPopularStocksByTrait(traitGroup);
    }
}