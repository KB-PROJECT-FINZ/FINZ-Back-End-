package org.scoula.util.chatbot;

import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Log4j2
public class PortfolioStatsUtil {

    public static BehaviorStatsDto calculate(List<TransactionDTO> transactions, int requestedPeriod) {
        if (transactions == null || transactions.isEmpty()) {
            return emptyStats();
        }

        List<TransactionDTO> sortedTx = new ArrayList<>(transactions);
        sortedTx.sort(Comparator.comparing(TransactionDTO::getExecutedAt));

        int buyCount = 0;
        int sellCount = 0;
        double buyTotal = 0.0;
        double sellTotal = 0.0;

        Map<String, Queue<Lot>> buyMap = new HashMap<>();
        long totalHoldDays = 0;
        int matchedQuantity = 0;

        for (TransactionDTO tx : sortedTx) {
            String code = tx.getStockCode();
            LocalDate date = tx.getExecutedAt().toLocalDate();
            int qty = tx.getQuantity();
            double amount = tx.getPrice() * qty;

            if ("BUY".equalsIgnoreCase(tx.getTransactionType())) {
                buyTotal += amount;
                buyCount++;
                buyMap.computeIfAbsent(code, k -> new LinkedList<>()).add(new Lot(date, qty));
            } else if ("SELL".equalsIgnoreCase(tx.getTransactionType())) {
                sellTotal += amount;
                sellCount++;
                Queue<Lot> lots = buyMap.get(code);
                if (lots != null) {
                    int remainingQty = qty;
                    while (remainingQty > 0 && !lots.isEmpty()) {
                        Lot lot = lots.peek();
                        int matchedQty = Math.min(remainingQty, lot.quantity);
                        long holdDays = ChronoUnit.DAYS.between(lot.buyDate, date);
                        if (holdDays >= 0) {
                            totalHoldDays += holdDays * matchedQty;
                            matchedQuantity += matchedQty;
                        } else {
                            log.warn("⛔️ 음수 보유일 발생: BUY={}, SELL={}", lot.buyDate, date);
                        }

                        if (lot.quantity > matchedQty) {
                            lot.quantity -= matchedQty;
                        } else {
                            lots.poll();
                        }
                        remainingQty -= matchedQty;
                    }
                }
            }
        }

        double totalReturn = buyTotal == 0 ? 0.0 : ((sellTotal - buyTotal) / buyTotal) * 100.0;
        double avgHoldDays = matchedQuantity == 0 ? 0.0 : (double) totalHoldDays / matchedQuantity;

        return BehaviorStatsDto.builder()
                .transactionCount(transactions.size())
                .totalReturn(Math.round(totalReturn * 100.0) / 100.0)
                .buyCount(buyCount)
                .sellCount(sellCount)
                .avgHoldDays(Math.round(avgHoldDays * 10.0) / 10.0)
                .requestedPeriod(requestedPeriod) // ✅ 여기에 설정
                .build();
    }

    private static BehaviorStatsDto emptyStats() {
        return BehaviorStatsDto.builder()
                .transactionCount(0)
                .totalReturn(0.0)
                .buyCount(0)
                .sellCount(0)
                .avgHoldDays(0.0)
                .build();
    }

    private static class Lot {
        LocalDate buyDate;
        int quantity;

        Lot(LocalDate buyDate, int quantity) {
            this.buyDate = buyDate;
            this.quantity = quantity;
        }
    }
}
