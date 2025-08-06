package org.scoula.util.chatbot;

import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PortfolioStatsUtil {

    public static BehaviorStatsDto buildStats(List<TransactionDTO> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return BehaviorStatsDto.builder()
                    .transactionCount(0)
                    .analysisPeriod(0)
                    .startDate(null)
                    .endDate(null)
                    .totalReturn(0.0)
                    .buyCount(0)
                    .sellCount(0)
                    .avgHoldDays(0.0)
                    .build();
        }

        // 날짜 정렬
        List<LocalDate> dates = transactions.stream()
                .map(t -> t.getExecutedAt().toLocalDate())
                .sorted()
                .toList();

        LocalDate start = dates.get(0);
        LocalDate end = dates.get(dates.size() - 1);
        int days = (int) ChronoUnit.DAYS.between(start, end);
        int analysisPeriod = days <= 0 ? 1 : days;

        int buyCount = 0;
        int sellCount = 0;
        double buyTotal = 0.0;
        double sellTotal = 0.0;

        // 🧠 보유일 계산용: 종목별 매수 날짜 저장
        Map<String, List<LocalDate>> buyDateMap = new HashMap<>();
        List<Long> holdDurations = new ArrayList<>();

        for (TransactionDTO t : transactions) {
            double amount = t.getPrice() * t.getQuantity();
            String code = t.getStockCode();
            LocalDate txDate = t.getExecutedAt().toLocalDate();

            if ("BUY".equalsIgnoreCase(t.getTransactionType())) {
                buyTotal += amount;
                buyCount++;

                buyDateMap.putIfAbsent(code, new ArrayList<>());
                buyDateMap.get(code).add(txDate);

            } else if ("SELL".equalsIgnoreCase(t.getTransactionType())) {
                sellTotal += amount;
                sellCount++;

                // 해당 종목의 가장 오래된 매수일과의 차이 계산
                if (buyDateMap.containsKey(code) && !buyDateMap.get(code).isEmpty()) {
                    LocalDate oldestBuy = buyDateMap.get(code).remove(0);  // FIFO
                    long holdDays = ChronoUnit.DAYS.between(oldestBuy, txDate);
                    holdDurations.add(holdDays);
                }
            }
        }

        double totalReturn = buyTotal == 0 ? 0.0 : ((sellTotal - buyTotal) / buyTotal) * 100.0;

        // 평균 보유일 계산
        double avgHoldDays = holdDurations.isEmpty()
                ? 0.0
                : holdDurations.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);

        return BehaviorStatsDto.builder()
                .transactionCount(transactions.size())
                .analysisPeriod(analysisPeriod)
                .startDate(start)
                .endDate(end)
                .totalReturn(Math.round(totalReturn * 100.0) / 100.0) // 소수점 둘째자리 반올림
                .buyCount(buyCount)
                .sellCount(sellCount)
                .avgHoldDays(Math.round(avgHoldDays * 10.0) / 10.0) // 평균 보유일도 1자리 반올림
                .build();
    }
}
