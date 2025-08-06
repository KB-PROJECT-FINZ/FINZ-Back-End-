package org.scoula.util.chatbot;

import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

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

        List<LocalDate> dates = transactions.stream()
                .map(t -> t.getExecutedAt().toLocalDate())
                .sorted()
                .toList();

        LocalDate start = dates.get(0);
        LocalDate end = dates.get(dates.size() - 1);
        int analysisPeriod = (int) ChronoUnit.DAYS.between(start, end) + 1;

        int buyCount = 0;
        int sellCount = 0;
        double buyTotal = 0.0;
        double sellTotal = 0.0;

        for (TransactionDTO t : transactions) {
            double amount = t.getPrice() * t.getQuantity();
            if ("BUY".equalsIgnoreCase(t.getTransactionType())) {
                buyTotal += amount;
                buyCount++;
            } else if ("SELL".equalsIgnoreCase(t.getTransactionType())) {
                sellTotal += amount;
                sellCount++;
            }
        }

        double totalReturn = buyTotal == 0 ? 0.0 : ((sellTotal - buyTotal) / buyTotal) * 100.0;

        return BehaviorStatsDto.builder()
                .transactionCount(transactions.size())
                .analysisPeriod(analysisPeriod)
                .startDate(start)
                .endDate(end)
                .totalReturn(Math.round(totalReturn * 100.0) / 100.0)
                .buyCount(buyCount)
                .sellCount(sellCount)
                .avgHoldDays(0.0) // 추후 구현 가능
                .build();
    }
}
