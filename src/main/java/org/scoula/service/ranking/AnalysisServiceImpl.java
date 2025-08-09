package org.scoula.service.ranking;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.api.mocktrading.PriceApi;
import org.scoula.domain.ranking.*;
import org.scoula.mapper.ranking.AnalysisMapper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
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
        // DB에 저장된 분포 데이터를 빠르게 조회해서 리턴
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

    @Override
    public void saveMyStockDistribution(Long userId, List<MyDistributionDto> distributions) {
        log.info("✅ [saveMyStockDistribution] userId = {}", userId);

        // 1️⃣ 전체 종목 분포 데이터 조회
        List<StockDistributionSummaryDto> overallDistributionsAll =
                analysisMapper.aggregateAllStockDistributions();

        log.info("📊 전체 종목 분포 사이즈: {}", overallDistributionsAll.size());

        for (MyDistributionDto dist : distributions) {
            String stockCode = dist.getStockCode();
            log.info("🔍 현재 처리 종목: {} ({})", stockCode, dist.getStockName());

            // 2️⃣ bin 데이터 찾기 (공백/대소문자 무시)
            StockDistributionSummaryDto binCounts = overallDistributionsAll.stream()
                    .filter(d -> d.getStockCode() != null &&
                            d.getStockCode().trim().equalsIgnoreCase(stockCode.trim()))
                    .findFirst()
                    .orElse(null);

            if (binCounts != null) {
                dist.setBin0(binCounts.getBin0());
                dist.setBin1(binCounts.getBin1());
                dist.setBin2(binCounts.getBin2());
                dist.setBin3(binCounts.getBin3());
                dist.setBin4(binCounts.getBin4());
                dist.setBin5(binCounts.getBin5());
                log.info("✅ bin 데이터 매핑 완료: {}", binCounts);
            } else {
                log.warn("⚠️ bin 데이터 없음 - stockCode = {}", stockCode);
            }

            // 3️⃣ 저장
            log.info("📦 최종 Distribution 저장: {}", dist);
            analysisMapper.upsertMyStockDistribution(userId, dist);
        }
    }


    // 매일 새벽 1시에 실행되는 스케줄러 메서드 (전체 수익률 및 사용자 분포 집계)
    @Scheduled(cron = "* 0 * * * ?")
    public void updateStockProfitRatesAndUserDistributions() {
        log.info("[스케줄러] 수익률 및 사용자 분포 집계 시작");

        try {
            // 1) 전체 종목별 보유 종목 평균 매입가 조회
            List<HoldingSummaryDto> allHoldings = analysisMapper.findHoldingSummaries();

            // 2) 종목별 실시간 가격 조회 및 수익률 계산 후 DB 저장
            for (HoldingSummaryDto holding : allHoldings) {
                String stockCode = holding.getStockCode();
                double avgPrice = holding.getAveragePrice();

                JsonNode priceData = PriceApi.getPriceData(stockCode);
                double currentPrice = priceData.path("output").path("stck_prpr").asDouble();

                double profitRate = avgPrice > 0 ? ((currentPrice - avgPrice) / avgPrice) * 100.0 : 0;

                analysisMapper.upsertStockProfitRate(stockCode, profitRate);
            }

            // 3) 전체 사용자 리스트 조회
            List<Integer> allUserIds = analysisMapper.findAllUserIds();

            // 4) 전체 분포 데이터 (bin별 카운트) 조회
            List<StockDistributionSummaryDto> overallDistributionsAll = analysisMapper.aggregateAllStockDistributions();

            // 5) 사용자별 분포 계산 및 DB 저장 반복
            for (int userId : allUserIds) {
                // 사용자 보유 종목 조회
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
                            .orElse(null);

                    if (binCounts == null) {
                        log.warn("[WARN] binCounts 없음 - stockCode: {}", stockCode);
                        binCounts = new StockDistributionSummaryDto(); // 기본값
                    }

                    log.info("[DEBUG] userId={}, stockCode={}, gainRate={}, binIndex={}, label={}, binCounts={}",
                            userId, stockCode, gainRate, binIndex, label, binCounts);

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

                // 사용자별 분포 DB에 upsert (저장)
                saveMyStockDistribution((long) userId, userDistributions);
            }

            log.info("[스케줄러] 수익률 및 사용자 분포 집계 완료");

        } catch (Exception e) {
            log.error("[스케줄러] 집계 중 오류 발생: {}", e.getMessage(), e);
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
