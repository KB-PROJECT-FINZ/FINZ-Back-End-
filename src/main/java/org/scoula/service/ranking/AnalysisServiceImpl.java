package org.scoula.service.ranking;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.api.mocktrading.PriceApi;
import org.scoula.domain.ranking.*;
import org.scoula.mapper.ranking.AnalysisMapper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

@Log4j2
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
        return analysisMapper.findPopularStocksByTrait(traitGroup);
    }

    @Override
    public void saveMyStockDistribution(Long userId, List<MyDistributionDto> distributions) {
        List<StockDistributionSummaryDto> overallDistributionsAll =
                analysisMapper.aggregateAllStockDistributions();

        for (MyDistributionDto dist : distributions) {
            StockDistributionSummaryDto binCounts = overallDistributionsAll.stream()
                    .filter(d -> d.getStockCode() != null
                            && d.getStockCode().trim().equalsIgnoreCase(dist.getStockCode().trim()))
                    .findFirst()
                    .orElse(null);

            if (binCounts != null) {
                dist.setBin0(binCounts.getBin0());
                dist.setBin1(binCounts.getBin1());
                dist.setBin2(binCounts.getBin2());
                dist.setBin3(binCounts.getBin3());
                dist.setBin4(binCounts.getBin4());
                dist.setBin5(binCounts.getBin5());
                dist.setColor(binCounts.getColor());
            }

            analysisMapper.upsertMyStockDistribution(userId, dist);
        }
    }

    // 매일 새벽 1시 실행 (cron은 기존 값 유지)
    @Scheduled(cron = "* 0 * * * ?")
    public void updateStockProfitRatesAndUserDistributions() {
        try {
            // 1) 전체 종목별 보유 종목 평균 매입가 조회
            List<HoldingSummaryDto> allHoldings = analysisMapper.findHoldingSummaries();

            // 2) 종목별 실시간 가격 조회 및 수익률 저장
            for (HoldingSummaryDto holding : allHoldings) {
                String stockCode = holding.getStockCode();
                double avgPrice = holding.getAveragePrice();

                JsonNode priceData = PriceApi.getPriceData(stockCode);
                double currentPrice = priceData.path("output").path("stck_prpr").asDouble();
                double profitRate = avgPrice > 0 ? ((currentPrice - avgPrice) / avgPrice) * 100.0 : 0;

                analysisMapper.upsertStockProfitRate(stockCode, profitRate);
            }

            // 3) 전체 사용자별 분포 계산 및 저장
            List<Integer> allUserIds = analysisMapper.findAllUserIds();
            List<StockDistributionSummaryDto> overallDistributionsAll =
                    analysisMapper.aggregateAllStockDistributions();

            for (int userId : allUserIds) {
                List<HoldingSummaryDto> userHoldings = analysisMapper.findHoldingsByUserId(userId);
                List<MyDistributionDto> userDistributions = new ArrayList<>();

                for (HoldingSummaryDto holding : userHoldings) {
                    String stockCode = holding.getStockCode();
                    double avgPrice = holding.getAveragePrice();

                    JsonNode priceData = PriceApi.getPriceData(stockCode);
                    double currentPrice = priceData.path("output").path("stck_prpr").asDouble();

                    double gainRate = avgPrice > 0 ? ((currentPrice - avgPrice) / avgPrice) * 100.0 : 0;
                    int binIndex = getBinIndex(gainRate);
                    String label = getBinLabel(binIndex);

                    StockDistributionSummaryDto binCounts = overallDistributionsAll.stream()
                            .filter(d -> d.getStockCode().equals(stockCode))
                            .findFirst()
                            .orElse(new StockDistributionSummaryDto());

                    MyDistributionDto dto = new MyDistributionDto();
                    dto.setStockCode(stockCode);
                    dto.setStockName(holding.getStockName());
                    dto.setGainRate(gainRate);
                    dto.setPositionIndex(binIndex);
                    dto.setPositionLabel(label);
                    dto.setBin0(binCounts.getBin0());
                    dto.setBin1(binCounts.getBin1());
                    dto.setBin2(binCounts.getBin2());
                    dto.setBin3(binCounts.getBin3());
                    dto.setBin4(binCounts.getBin4());
                    dto.setBin5(binCounts.getBin5());
                    dto.setColor(binCounts.getColor());

                    userDistributions.add(dto);
                }

                saveMyStockDistribution((long) userId, userDistributions);
            }
        } catch (Exception e) {
            log.error("스케줄러 집계 오류", e);
        }
    }

    private int getBinIndex(double rate) {
        if (rate < -20) return 0;
        if (rate < 0) return 1;
        if (rate < 10) return 2;
        if (rate < 20) return 3;
        if (rate < 50) return 4;
        return 5;
    }

    private String getBinLabel(int index) {
        return switch (index) {
            case 0 -> "-20%↓";
            case 1 -> "-20%~0%";
            case 2 -> "0%~10%";
            case 3 -> "10%~20%";
            case 4 -> "20%~50%";
            case 5 -> "50%↑";
            default -> "";
        };
    }
}