package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.mapper.ranking.AssetHistoryMapper;
import org.scoula.mapper.ranking.HoldingsMapper;
import org.scoula.mapper.ranking.UserAccountsMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class AssetHistoryServiceImpl implements AssetHistoryService {

    private final UserAccountsMapper userAccountsMapper;
    private final HoldingsMapper holdingsMapper;
    private final AssetHistoryMapper assetHistoryMapper;
    private final StockPriceService stockPriceService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 오늘 기준 '지난주 일요일'(주간 앵커) */
    private LocalDate lastSundayKst() {
        LocalDate today = LocalDate.now(KST);
        // 항상 '이전' 일요일로 이동 (오늘이 일요일이어도 지난주로 간주)
        return today.with(TemporalAdjusters.previous(java.time.DayOfWeek.SUNDAY));
    }

    /** 이번 주 일요일(수동 스냅샷용) */
    private LocalDate thisSundayKst() {
        LocalDate today = LocalDate.now(KST);
        return today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
    }

    // =============== Public APIs ===============

    /** 주간 데이터 저장 (기본: 지난주 일요일, KST 기준) */
    @Override
    public void saveWeeklyAssetHistory() {
        LocalDate baseDate = lastSundayKst();
        log.info(">>> [WEEKLY] baseDate (last Sunday, KST) = {}", baseDate);
        saveForAllAccounts(baseDate);
    }

    /** 주간 데이터 저장 (앵커 일요일을 명시적으로 지정) */
    @Override
    public void saveWeeklyAssetHistory(LocalDate anchorSunday) {
        LocalDate baseDate = (anchorSunday != null) ? anchorSunday : lastSundayKst();
        log.info(">>> [WEEKLY] baseDate (explicit anchor, KST) = {}", baseDate);
        saveForAllAccounts(baseDate);
    }

    /** (기존) 이번 주 자산 스냅샷 저장 — 필요 시 유지 */
    @Override
    public void saveAssetHistoryForAllUsersThisWeek() {
        LocalDate baseDate = thisSundayKst();
        log.info(">>> [THIS WEEK] baseDate (this Sunday, KST) = {}", baseDate);
        saveForAllAccounts(baseDate);
    }

    // =============== Internal ===============

    private void saveForAllAccounts(LocalDate baseDate) {
        List<Integer> accountIds = userAccountsMapper.selectAllAccountIds();
        for (Integer accountId : accountIds) {
            log.info(">>> saving asset history: accountId={}, baseDate={}", accountId, baseDate);
            saveAssetHistoryForAccountAndDate(accountId, baseDate);
        }
        log.info(">>> asset history save completed (anchor={})", baseDate);
    }

    /**
     * ✅ 수익률 계산: 순기여금 = 초기자본(임시)
     *  - 새 인터페이스/레저 없이 현재 제출 요건에 맞춰 간소화
     *  - 나중에 ledger(입금/출금/크레딧) 붙일 땐 netContrib만 확장
     */
    private void saveAssetHistoryForAccountAndDate(Integer accountId, LocalDate baseDate) {
        try {
            // (1) 주식 평가금액 계산
            var holdings = holdingsMapper.selectByAccountId(accountId);
            List<String> stockCodes = holdings.stream()
                    .map(h -> h.getStockCode())
                    .collect(Collectors.toList());

            Map<String, BigDecimal> priceMap = stockPriceService.getCurrentPrices(stockCodes);

            BigDecimal stockValue = ZERO;
            for (var h : holdings) {
                BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
                BigDecimal cur = priceMap.getOrDefault(h.getStockCode(), ZERO);
                stockValue = stockValue.add(cur.multiply(qty));
            }

            // (2) 현금 + 총자산
            BigDecimal cash = nvl(userAccountsMapper.selectCurrentBalance(accountId), ZERO);
            BigDecimal totalAsset = stockValue.add(cash);

            // (3) 순기여금 = 초기자본(기본값 없음, NULL은 0 처리)
            BigDecimal initialCapital = nvl(userAccountsMapper.selectInitialCapital(accountId), ZERO);
            BigDecimal netContrib = initialCapital;

            // (4) 손익/수익률 계산
            BigDecimal cumPnL = totalAsset.subtract(netContrib);
            BigDecimal profitRate = netContrib.signum() > 0
                    ? cumPnL.multiply(new BigDecimal("100"))
                    .divide(netContrib, 2, RoundingMode.HALF_UP)
                    : ZERO;

            // (5) 저장 (dailyPnL은 간단히 누적손익으로 저장; 필요 시 전일 대비로 교체)
            BigDecimal dailyPnL = cumPnL;

            // XML이 parameterType="map"이므로 Map으로 바인딩
            Map<String, Object> params = new HashMap<>();
            params.put("accountId", accountId);
            params.put("baseDate", Date.valueOf(baseDate));     // java.sql.Date로 안전 매핑
            params.put("totalAssetValue", totalAsset);
            params.put("cashBalance", cash);
            params.put("stockValue", stockValue);
            params.put("profitLoss", dailyPnL);                 // daily_profit_loss
            params.put("realizedProfitLoss", cumPnL);           // cumulative_profit_loss
            params.put("profitRate", profitRate);

            assetHistoryMapper.insertAssetHistory(params);

            log.info("✅ saved — accountId={}, baseDate={}, totalAsset={}, netContrib(initial)={}, cumPnL={}, rate={}%",
                    accountId, baseDate, totalAsset, netContrib, cumPnL, profitRate);

        } catch (Exception e) {
            log.error("❌ save failed — accountId={}, baseDate={}, err={}", accountId, baseDate, e.getMessage(), e);
        }
    }

    private static BigDecimal nvl(BigDecimal v, BigDecimal d) {
        return v == null ? d : v;
    }
}

