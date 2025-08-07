package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.mapper.ranking.AssetHistoryMapper;
import org.scoula.mapper.ranking.HoldingsMapper;
import org.scoula.mapper.ranking.UserAccountsMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class AssetHistoryServiceImpl implements AssetHistoryService {

    private final UserAccountsMapper userAccountsMapper;
    private final HoldingsMapper holdingsMapper;
    private final AssetHistoryMapper assetHistoryMapper;
    private final StockPriceService stockPriceService;

    @Override
    public void saveWeeklyAssetHistory() {
        LocalDate baseDate = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .minusWeeks(1);
        log.info(">>> 기준 날짜 (지난주 월요일): {}", baseDate);

        List<Integer> accountIds = userAccountsMapper.selectAllAccountIds();
        log.info(">>> 전체 계좌 수: {}", accountIds.size());

        for (Integer accountId : accountIds) {
            log.info(">>> 계좌 처리 시작: accountId={}", accountId);
            try {
                var holdings = holdingsMapper.selectByAccountId(accountId);
                log.info(">>> 보유 종목 수: {}", holdings.size());

                List<String> stockCodes = holdings.stream()
                        .map(h -> h.getStockCode())
                        .toList();

                Map<String, BigDecimal> currentPrices = stockPriceService.getCurrentPrices(stockCodes);

                BigDecimal totalStockValue = BigDecimal.ZERO;
                BigDecimal totalInvestment = BigDecimal.ZERO;

                for (var holding : holdings) {
                    BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());
                    BigDecimal avgPrice = holding.getAveragePrice();

                    BigDecimal currentPrice = currentPrices.getOrDefault(holding.getStockCode(), BigDecimal.ZERO);

                    BigDecimal currentValue = currentPrice.multiply(quantity);
                    BigDecimal investedAmount = avgPrice.multiply(quantity);

                    totalStockValue = totalStockValue.add(currentValue);
                    totalInvestment = totalInvestment.add(investedAmount);
                }

                BigDecimal currentBalance = userAccountsMapper.selectCurrentBalance(accountId);

                BigDecimal totalAssetValue = totalStockValue.add(currentBalance);

                BigDecimal totalProfitLoss = totalAssetValue.subtract(totalInvestment);
                BigDecimal profitRate = BigDecimal.ZERO;
                if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
                    profitRate = totalProfitLoss.multiply(BigDecimal.valueOf(100))
                            .divide(totalInvestment, 2, BigDecimal.ROUND_HALF_UP);
                }

                assetHistoryMapper.insertAssetHistory(
                        accountId,
                        baseDate,
                        totalAssetValue,
                        currentBalance,
                        totalStockValue,
                        totalProfitLoss,
                        totalProfitLoss,  // 필요 시 분리 가능
                        profitRate
                );

                log.info("✅ 자산 이력 저장 완료 - 계좌 ID: {}, 자산가치: {}, 수익률: {}%", accountId, totalAssetValue, profitRate);

            } catch (Exception e) {
                log.error("❌ 자산 이력 저장 실패 - 계좌 ID: {}, 에러: {}", accountId, e.getMessage(), e);
            }
        }

        log.info(">>> 자산 이력 저장 작업 완료");
    }
}
