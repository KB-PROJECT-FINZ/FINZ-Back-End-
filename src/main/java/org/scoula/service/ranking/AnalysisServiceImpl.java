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
        // ✅ bin 합계는 조회 시 집계하므로, 여기서는 개인 스냅샷(수익률/포지션/색상)만 저장
        for (MyDistributionDto dist : distributions) {
            analysisMapper.upsertMyStockDistribution(userId, dist);
        }
    }

    /** 매일 새벽 1시 실행 */
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateStockProfitRatesAndUserDistributions() {
        try {
            // 1) 전체 종목 평균 매입가
            List<HoldingSummaryDto> allHoldings = analysisMapper.findHoldingSummaries();

            // 2) 종목별 실시간 가격 → 수익률 저장
            for (HoldingSummaryDto holding : allHoldings) {
                String stockCode = holding.getStockCode();
                double avgPrice  = holding.getAveragePrice();

                JsonNode priceData = PriceApi.getPriceData(stockCode);
                double currentPrice = priceData.path("output").path("stck_prpr").asDouble();
                double profitRate = avgPrice > 0 ? ((currentPrice - avgPrice) / avgPrice) * 100.0 : 0;

                analysisMapper.upsertStockProfitRate(stockCode, profitRate);
            }

            // 3) 사용자별 분포 스냅샷 저장 (bin은 저장하지 않아도 OK)
            List<Integer> allUserIds = analysisMapper.findAllUserIds();

            for (int userId : allUserIds) {
                List<HoldingSummaryDto> userHoldings = analysisMapper.findHoldingsByUserId(userId);
                List<MyDistributionDto> userDistributions = new ArrayList<>();

                for (HoldingSummaryDto holding : userHoldings) {
                    String stockCode = holding.getStockCode();
                    double avgPrice  = holding.getAveragePrice();

                    JsonNode priceData = PriceApi.getPriceData(stockCode);
                    double currentPrice = priceData.path("output").path("stck_prpr").asDouble();

                    double gainRate = avgPrice > 0 ? ((currentPrice - avgPrice) / avgPrice) * 100.0 : 0;
                    int binIndex = getBinIndex(gainRate);
                    String label = getBinLabel(binIndex);

                    MyDistributionDto dto = new MyDistributionDto();
                    dto.setStockCode(stockCode);
                    dto.setStockName(holding.getStockName()); // null이어도 조회 시 COALESCE 처리
                    dto.setGainRate(gainRate);
                    dto.setPositionIndex(binIndex);
                    dto.setPositionLabel(label);
                    // bin0~5는 저장하지 않음 (집계 쿼리에서 최신 스냅샷 기준 합계)
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

