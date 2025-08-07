package org.scoula.util.chatbot;

import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Log4j2
public class PortfolioStatsUtil {

    public static BehaviorStatsDto calculate(List<TransactionDTO> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return emptyStats();
        }

        // 거래 날짜 정렬
        transactions.sort(Comparator.comparing(TransactionDTO::getExecutedAt));
        LocalDate startDate = transactions.get(0).getExecutedAt().toLocalDate();
        LocalDate endDate = transactions.get(transactions.size() - 1).getExecutedAt().toLocalDate();
        int analysisPeriod = (int) Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));

        int buyCount = 0;
        int sellCount = 0;
        double buyTotal = 0.0;
        double sellTotal = 0.0;

        Map<String, Queue<LocalDate>> buyMap = new HashMap<>();
        long totalHoldDays = 0;
        int matchedPairs = 0;

        for (TransactionDTO tx : transactions) {
            String code = tx.getStockCode();
            LocalDate txDate = tx.getExecutedAt().toLocalDate();
            double amount = tx.getPrice() * tx.getQuantity();

            if ("BUY".equalsIgnoreCase(tx.getTransactionType())) {
                buyTotal += amount;
                buyCount++;
                buyMap.computeIfAbsent(code, k -> new LinkedList<>()).add(txDate);

            } else if ("SELL".equalsIgnoreCase(tx.getTransactionType())) {
                sellTotal += amount;
                sellCount++;
                Queue<LocalDate> queue = buyMap.get(code);

                if (queue != null && !queue.isEmpty()) {
                    LocalDate buyDate = queue.poll();
                    long holdDays = ChronoUnit.DAYS.between(buyDate, txDate);
                    if (holdDays >= 0) {
                        totalHoldDays += holdDays;
                        matchedPairs++;
                    } else {
                        log.warn("⛔️ 음수 보유일 발생: BUY={}, SELL={}", buyDate, txDate);
                    }
                }
            }
        }

        double totalReturn = (buyTotal == 0) ? 0.0 : ((sellTotal - buyTotal) / buyTotal) * 100.0;
        double avgHoldDays = matchedPairs == 0 ? 0.0 : (double) totalHoldDays / matchedPairs;

        return BehaviorStatsDto.builder()
                .transactionCount(transactions.size())
                .analysisPeriod(analysisPeriod)
                .startDate(startDate)
                .endDate(endDate)
                .totalReturn(Math.round(totalReturn * 100.0) / 100.0)
                .buyCount(buyCount)
                .sellCount(sellCount)
                .avgHoldDays(Math.round(avgHoldDays * 10.0) / 10.0)
                .build();
    }

    private static BehaviorStatsDto emptyStats() {
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
}