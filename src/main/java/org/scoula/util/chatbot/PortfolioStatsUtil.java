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
                    .startDate("N/A")
                    .endDate("N/A")
                    .totalReturn(0.0)
                    .build();
        }

        LocalDate start = transactions.stream()
                .map(t -> LocalDate.parse(t.getExecutedAt().substring(0, 10)))
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        LocalDate end = transactions.stream()
                .map(t -> LocalDate.parse(t.getExecutedAt().substring(0, 10)))
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        int analysisPeriod = (int) ChronoUnit.DAYS.between(start, end) + 1;
        int transactionCount = transactions.size();

        double totalBuy = transactions.stream()
                .filter(t -> "BUY".equalsIgnoreCase(t.getTransactionType()))
                .mapToDouble(t -> t.getPrice() * t.getQuantity())
                .sum();

        double totalSell = transactions.stream()
                .filter(t -> "SELL".equalsIgnoreCase(t.getTransactionType()))
                .mapToDouble(t -> t.getPrice() * t.getQuantity())
                .sum();

        double totalReturn = totalBuy == 0 ? 0.0 : ((totalSell - totalBuy) / totalBuy) * 100.0;

        return BehaviorStatsDto.builder()
                .transactionCount(transactionCount)
                .analysisPeriod(analysisPeriod)
                .startDate(start.toString())
                .endDate(end.toString())
                .totalReturn(totalReturn)
                .build();
    }
}
