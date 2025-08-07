package org.scoula.util.chatbot;

import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Log4j2
public class PortfolioStatsUtil {

    public static BehaviorStatsDto calculateWithQuantity(List<TransactionDTO> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return emptyStats();
        }

        List<TransactionDTO> sortedTransactions = new ArrayList<>(transactions);
        sortedTransactions.sort(Comparator.comparing(TransactionDTO::getExecutedAt));        LocalDate startDate = transactions.get(0).getExecutedAt().toLocalDate();
        LocalDate endDate = transactions.get(transactions.size() - 1).getExecutedAt().toLocalDate();
        int analysisPeriod = (int) Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));

        int buyCount = 0;
        int sellCount = 0;
        double buyTotal = 0.0;
        double sellTotal = 0.0;

        Map<String, Queue<BuyLot>> buyMap = new HashMap<>();
        long totalHoldDays = 0;
        int totalMatchedQty = 0;

        for (TransactionDTO tx : transactions) {
            String code = tx.getStockCode();
            LocalDate txDate = tx.getExecutedAt().toLocalDate();
            int quantity = tx.getQuantity();
            double amount = tx.getPrice() * quantity;

            if ("BUY".equalsIgnoreCase(tx.getTransactionType())) {
                buyTotal += amount;
                buyCount++;

                buyMap.computeIfAbsent(code, k -> new LinkedList<>())
                        .add(new BuyLot(txDate, quantity));

            } else if ("SELL".equalsIgnoreCase(tx.getTransactionType())) {
                sellTotal += amount;
                sellCount++;

                Queue<BuyLot> queue = buyMap.getOrDefault(code, new LinkedList<>());
                int remaining = quantity;

                while (remaining > 0 && !queue.isEmpty()) {
                    BuyLot lot = queue.peek();
                    int matchedQty = Math.min(remaining, lot.qty);
                    long holdDays = ChronoUnit.DAYS.between(lot.date, txDate);

                    if (holdDays >= 0) {
                        totalHoldDays += holdDays * matchedQty;
                        totalMatchedQty += matchedQty;
                    } else {
                        log.warn("⛔ 음수 보유일 발생: BUY={}, SELL={}", lot.date, txDate);
                    }

                    lot.qty -= matchedQty;
                    remaining -= matchedQty;

                    if (lot.qty == 0) queue.poll(); // 다 쓴 lot 제거
                }

                if (remaining > 0) {
                    log.warn("⚠ 남은 미매칭 매도 수량: {} (종목: {})", remaining, code);
                }
            }
        }

        double totalReturn = (buyTotal == 0) ? 0.0 : ((sellTotal - buyTotal) / buyTotal) * 100.0;
        double avgHoldDays = (totalMatchedQty == 0) ? 0.0 : (double) totalHoldDays / totalMatchedQty;

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

    // 내부 클래스: 매수 Lot
    private static class BuyLot {
        LocalDate date;
        int qty;

        BuyLot(LocalDate date, int qty) {
            this.date = date;
            this.qty = qty;
        }
    }
}
