package org.scoula.controller.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.ranking.MyDistributionDto;
import org.scoula.domain.ranking.MyDistributionRequest;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;
import org.scoula.service.ranking.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Log4j2
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

    // (2) 내 수익률 분포 위치 조회 (DB에서 저장된 결과 조회)
    @GetMapping("/my-distribution")
    public List<MyDistributionDto> getMyDistribution(@RequestParam Long userId) {
        List<MyDistributionDto> result = analysisService.getMyDistribution(userId);
        log.info("[getMyDistribution] 조회된 결과 크기 = {}", result == null ? "null" : result.size());

        if (result != null) {
            for (MyDistributionDto dto : result) {
                dto.setDistributionBins(Arrays.asList(
                        Optional.ofNullable(dto.getBin0()).orElse(0),
                        Optional.ofNullable(dto.getBin1()).orElse(0),
                        Optional.ofNullable(dto.getBin2()).orElse(0),
                        Optional.ofNullable(dto.getBin3()).orElse(0),
                        Optional.ofNullable(dto.getBin4()).orElse(0),
                        Optional.ofNullable(dto.getBin5()).orElse(0)
                ));
                log.info("[getMyDistribution] {} bins = {}", dto.getStockCode(), dto.getDistributionBins());
            }
        }
        return result;
    }

    // (3) 유사 성향 투자자 인기 종목 조회
    @GetMapping("/popular-stocks")
    public List<PopularStockDto> getPopularStocks(@RequestParam String traitGroup) {
        String normalized = normalizeTraitGroup(traitGroup);
        log.info("[popular-stocks] traitGroup={} -> normalized={}", traitGroup, normalized);
        try {
            return analysisService.getPopularStocksByTrait(normalized);
        } catch (Exception e) {
            log.error("popular-stocks error: {}", e.getMessage(), e);
            return List.of(); // 500 대신 빈 배열
        }
    }

    private String normalizeTraitGroup(String tg) {
        if (tg == null) return "BALANCED";
        String x = tg.trim().toUpperCase();
        return switch (x) {
            case "AGR", "DTA", "EXP", "THE" -> "AGGRESSIVE";
            case "AID", "BGT", "BSS" -> "BALANCED";
            case "CAG", "CSD", "IND", "VAL" -> "CONSERVATIVE";
            case "INF", "SYS", "TEC" -> "ANALYTICAL";
            case "SOC" -> "EMOTIONAL";
            default -> x;
        };
    }

    // (4) 수익률 분포 저장 API (옵션)
    @PostMapping("/my-distribution/save")
    public ResponseEntity<?> saveMyStockDistribution(@RequestBody MyDistributionRequest request) {
        analysisService.saveMyStockDistribution(request.getUserId().longValue(), request.getDistributions());
        return ResponseEntity.ok().build();
    }

}


