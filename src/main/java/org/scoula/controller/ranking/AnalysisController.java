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

    @Autowired
    private AnalysisService analysisService;

    // 성향별 보유 비중
    @GetMapping("/trait-stock")
    public List<TraitStockDto> getTraitStocks() {
        return analysisService.getTraitStocks();
    }

    // 내 수익률 분포 위치
    @GetMapping("/my-distribution")
    public List<MyDistributionDto> getMyDistribution(@RequestParam Long userId) {
        return analysisService.getMyDistribution(userId);
    }

    // 유사 성향 투자자 인기 종목
    @GetMapping("/popular-stocks")
    public List<PopularStockDto> getPopularStocks(@RequestParam String trait) {
        return analysisService.getPopularStocksByTrait(trait);
    }
}
