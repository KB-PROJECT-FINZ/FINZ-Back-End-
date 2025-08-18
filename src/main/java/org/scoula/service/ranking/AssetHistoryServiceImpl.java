package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.mapper.ranking.AssetHistoryMapper;
import org.scoula.mapper.ranking.HoldingsMapper;
import org.scoula.mapper.ranking.UserAccountsMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
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

    /** KST 고정 */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 오늘 기준 ‘지난주 일요일’ (일~토 완결 주) */
    private LocalDate lastSundayKST() {
        LocalDate today = LocalDate.now(KST);
        int dow0 = today.getDayOfWeek().getValue() % 7; // SUN=0
        // “지난주” 일요일: 이번 주 일요일이더라도 반드시 1주 전으로
        return today.minusDays(dow0 + 7L);
    }

    /** 임의 날짜를 그 주의 ‘일요일’로 정규화 */
    private LocalDate normalizeToSunday(LocalDate d) {
        int dow0 = d.getDayOfWeek().getValue() % 7; // SUN=0
        return d.minusDays(dow0);
    }

    /* =========================
       퍼블릭 API 구현
       ========================= */

    /** (기존) 이번 주 월요일 기준 스냅샷 저장 — 필요 시 유지 */
    @Override
    public void saveAssetHistoryForAllUsersThisWeek() {
        // KST 기준 이번 주 월요일
        LocalDate thisWeekMonday = LocalDate.now(KST).with(DayOfWeek.MONDAY);
        List<Integer> accountIds = userAccountsMapper.selectAllAccountIds();
        for (Integer accountId : accountIds) {
            saveAssetHistoryForAccountAndDate(accountId, thisWeekMonday);
        }
    }

    /** 주간 데이터 저장 (기본: 지난주 일요일, KST) */
    @Override
    public void saveWeeklyAssetHistory() {
        LocalDate anchorSunday = lastSundayKST(); // 예: 2025-08-10
        log.info(">>> [WEEKLY] anchorSunday(default KST last Sunday) = {}", anchorSunday);

        List<Integer> accountIds = userAccountsMapper.selectAllAccountIds();
        log.info(">>> [WEEKLY] total accounts = {}", accountIds.size());

        for (Integer accountId : accountIds) {
            log.info(">>> [WEEKLY] processing accountId={}", accountId);
            saveAssetHistoryForAccountAndDate(accountId, anchorSunday);
        }
        log.info(">>> [WEEKLY] asset history save completed (anchor={})", anchorSunday);
    }

    /** 주간 데이터 저장 (앵커 일요일을 명시적으로 지정) */
    @Override
    public void saveWeeklyAssetHistory(LocalDate anchorSunday) {
        if (anchorSunday == null) {
            anchorSunday = lastSundayKST();
        } else {
            anchorSunday = normalizeToSunday(anchorSunday);
        }
        log.info(">>> [WEEKLY] anchorSunday(explicit) = {}", anchorSunday);

        List<Integer> accountIds = userAccountsMapper.selectAllAccountIds();
        log.info(">>> [WEEKLY] total accounts = {}", accountIds.size());

        for (Integer accountId : accountIds) {
            log.info(">>> [WEEKLY] processing accountId={}", accountId);
            saveAssetHistoryForAccountAndDate(accountId, anchorSunday);
        }
        log.info(">>> [WEEKLY] asset history save completed (anchor={})", anchorSunday);
    }

    /* =========================
       내부 헬퍼
       ========================= */

    /** 계좌별 자산 이력 저장 — baseDate(=anchorSunday or 월요일 등)를 그대로 저장 */
    private void saveAssetHistoryForAccountAndDate(Integer accountId, LocalDate baseDate) {
        try {
            var holdings = holdingsMapper.selectByAccountId(accountId);
            int holdingsCount = (holdings == null) ? 0 : holdings.size();
            log.info(">>> holdings count for accountId {}: {}", accountId, holdingsCount);

            List<String> stockCodes = (holdings == null) ? List.of()
                    : holdings.stream().map(h -> h.getStockCode()).toList();

            Map<String, BigDecimal> currentPrices =
                    stockPriceService.getCurrentPrices(stockCodes != null ? stockCodes : List.of());

            BigDecimal totalStockValue = BigDecimal.ZERO;
            BigDecimal totalInvestment = BigDecimal.ZERO;

            if (holdings != null) {
                for (var holding : holdings) {
                    BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());
                    BigDecimal avgPrice = holding.getAveragePrice() != null ? holding.getAveragePrice() : BigDecimal.ZERO;
                    BigDecimal currentPrice = currentPrices.getOrDefault(holding.getStockCode(), BigDecimal.ZERO);

                    BigDecimal currentValue = currentPrice.multiply(quantity);
                    BigDecimal investedAmount = avgPrice.multiply(quantity);

                    totalStockValue = totalStockValue.add(currentValue);
                    totalInvestment = totalInvestment.add(investedAmount);
                }
            }

            BigDecimal currentBalance = userAccountsMapper.selectCurrentBalance(accountId);
            if (currentBalance == null) currentBalance = BigDecimal.ZERO;

            BigDecimal totalAssetValue = totalStockValue.add(currentBalance);
            BigDecimal totalProfitLoss = totalAssetValue.subtract(totalInvestment);

            BigDecimal profitRate = BigDecimal.ZERO;
            if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
                profitRate = totalProfitLoss
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalInvestment, 2, RoundingMode.HALF_UP);
            }

            // ✅ baseDate 가 곧 ‘주차 앵커’로 저장됨 (주간이면 '일요일'이어야 함)
            assetHistoryMapper.insertAssetHistory(
                    accountId,
                    baseDate,
                    totalAssetValue,
                    currentBalance,
                    totalStockValue,
                    totalProfitLoss,
                    totalProfitLoss,  // 필요시 분리 가능
                    profitRate
            );

            log.info("✅ saved asset history — accountId={}, baseDate={}, totalAsset={}, profitRate={}%",
                    accountId, baseDate, totalAssetValue, profitRate);

        } catch (Exception e) {
            log.error("❌ failed to save asset history — accountId={}, baseDate={}, err={}",
                    accountId, baseDate, e.getMessage(), e);
        }
    }
}