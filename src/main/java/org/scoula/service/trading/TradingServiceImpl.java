package org.scoula.service.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.mapper.TradingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class TradingServiceImpl implements TradingService {

    private final TradingMapper tradingMapper;

    @Override
    public List<TransactionDTO> getUserTransactions(int userId) {
        return tradingMapper.getUserTransactions(userId);
    }

    @Override
    public BehaviorStatsDto summarizeUserBehavior(int userId) {
        List<TransactionDTO> transactions = tradingMapper.getUserTransactions(userId);

        return calculateBehaviorStats(transactions);
    }

    @Override
    public BehaviorStatsDto getBehaviorStats(Integer userId) {
        return summarizeUserBehavior(userId);
    }

    // ✅ 1. 분석 기간 고려한 거래 요약 통계
    @Override
    public BehaviorStatsDto getBehaviorStats(Integer userId, int periodDays) {
        List<TransactionDTO> allTx = getUserTransactions(userId);
        LocalDate cutoff = LocalDate.now().minusDays(periodDays);

        List<TransactionDTO> filtered = allTx.stream()
                .filter(tx -> tx.getExecutedAt().toLocalDate().isAfter(cutoff))
                .collect(Collectors.toList());

        log.info("📊 [기간 필터] 거래 수 ({}일): {}건", periodDays, filtered.size());

        return calculateBehaviorStats(filtered, periodDays);
    }

    // ✅ 2. 기간 필터링된 거래 ID 목록
    @Override
    public List<Long> getTransactionIdsByUser(Integer userId, int periodDays) {
        List<TransactionDTO> allTx = getUserTransactions(userId);
        LocalDate cutoff = LocalDate.now().minusDays(periodDays);

        return allTx.stream()
                .filter(tx -> tx.getExecutedAt().toLocalDate().isAfter(cutoff))
                .map(tx -> (long) tx.getTransactionId())  // ✅ int → long 캐스팅
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getTransactionIdsByUser(Integer userId) {
        return tradingMapper.getTransactionIdsByUser(userId);
    }

    // ✅ 거래 리스트 → BehaviorStatsDto로 계산
    private BehaviorStatsDto calculateBehaviorStats(List<TransactionDTO> transactions) {
        return calculateBehaviorStats(transactions, null);
    }

    // ✅ 오버로딩: 분석 기간까지 함께 저장
    private BehaviorStatsDto calculateBehaviorStats(List<TransactionDTO> transactions, Integer periodDaysOverride) {
        if (transactions == null || transactions.isEmpty()) {
            log.warn("❗ 거래내역 없음");
            return BehaviorStatsDto.builder()
                    .transactionCount(0)
                    .analysisPeriod(periodDaysOverride != null ? periodDaysOverride : 0)
                    .totalReturn(0.0)
                    .startDate(null)
                    .endDate(null)
                    .buyCount(0)
                    .sellCount(0)
                    .avgHoldDays(0.0)
                    .build();
        }

        // 날짜 계산
        TransactionDTO first = transactions.get(transactions.size() - 1);
        TransactionDTO last = transactions.get(0);

        LocalDateTime startDateTime = first.getExecutedAt();
        LocalDateTime endDateTime = last.getExecutedAt();

        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        int analysisPeriod = (int) Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));
        if (periodDaysOverride != null) {
            analysisPeriod = periodDaysOverride;
        }

        log.info("📅 분석 기간: {} ~ {} → {}일", startDate, endDate, analysisPeriod);

        // 수익률 및 매수/매도 계산
        double buyAmount = 0.0;
        double sellAmount = 0.0;
        int buyCount = 0;
        int sellCount = 0;

        for (TransactionDTO t : transactions) {
            double amount = t.getPrice() * t.getQuantity();
            if ("BUY".equalsIgnoreCase(t.getTransactionType())) {
                buyAmount += amount;
                buyCount++;
            } else if ("SELL".equalsIgnoreCase(t.getTransactionType())) {
                sellAmount += amount;
                sellCount++;
            }
        }

        double returnSum = sellAmount - buyAmount;
        double rate = buyAmount == 0 ? 0.0 : (returnSum / buyAmount) * 100;
        log.info("💰 총 매수: {}, 총 매도: {}, 수익률: {}%", buyAmount, sellAmount, rate);

        // 평균 보유일 계산
        double totalHoldingDays = 0.0;
        int holdingPairs = 0;
        Map<String, LocalDateTime> buyMap = new HashMap<>();

        for (TransactionDTO tx : transactions) {
            String code = tx.getStockCode();
            LocalDateTime time = tx.getExecutedAt();

            if ("BUY".equalsIgnoreCase(tx.getTransactionType())) {
                buyMap.put(code, time);
            } else if ("SELL".equalsIgnoreCase(tx.getTransactionType())) {
                if (buyMap.containsKey(code)) {
                    LocalDateTime buyTime = buyMap.get(code);
                    long hold = ChronoUnit.DAYS.between(buyTime, time);
                    totalHoldingDays += hold;
                    holdingPairs++;
                    buyMap.remove(code);
                }
            }
        }

        double avgHoldDays = holdingPairs == 0 ? 0.0 : totalHoldingDays / holdingPairs;
        log.info("⏳ 평균 보유일: {}일 (매수-매도 쌍: {})", avgHoldDays, holdingPairs);

        return BehaviorStatsDto.builder()
                .transactionCount(transactions.size())
                .analysisPeriod(analysisPeriod)
                .startDate(startDate)
                .endDate(endDate)
                .totalReturn(Math.round(rate * 100.0) / 100.0)
                .buyCount(buyCount)
                .sellCount(sellCount)
                .avgHoldDays(avgHoldDays)
                .build();
    }
}
