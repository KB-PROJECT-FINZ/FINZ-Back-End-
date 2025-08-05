package org.scoula.service.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.mapper.TradingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

        if (transactions == null || transactions.isEmpty()) {
            log.warn("❗ 거래내역 없음: userId = {}", userId);
            return BehaviorStatsDto.builder()
                    .transactionCount(0)
                    .analysisPeriod(0)
                    .totalReturn(0.0)
                    .startDate(null)
                    .endDate(null)
                    .build();
        }

        // 거래 날짜 범위 계산
        TransactionDTO first = transactions.get(transactions.size() - 1);
        TransactionDTO last = transactions.get(0);

        LocalDateTime start = first.getExecutedAt();
        LocalDateTime end = last.getExecutedAt();

        long days = ChronoUnit.DAYS.between(start, end);
        log.info("📅 거래 분석 기간: {} ~ {} → {}일", start, end, days);

        // 수익률 계산 (매수/매도 기준)
        double buyAmount = 0.0;
        double sellAmount = 0.0;

        for (TransactionDTO t : transactions) {
            double amount = t.getPrice() * t.getQuantity();
            if ("BUY".equalsIgnoreCase(t.getTransactionType())) {
                buyAmount += amount;
            } else if ("SELL".equalsIgnoreCase(t.getTransactionType())) {
                sellAmount += amount;
            }
        }

        double returnSum = sellAmount - buyAmount;
        double rate = buyAmount == 0 ? 0.0 : (returnSum / buyAmount) * 100;

        log.info("💰 총 매수: {}, 총 매도: {}, 수익률: {}%", buyAmount, sellAmount, rate);

        return BehaviorStatsDto.builder()
                .transactionCount(transactions.size())
                .analysisPeriod((int) days)
                .startDate(start.toLocalDate())  // LocalDate로 바로 전달
                .endDate(end.toLocalDate())
                .totalReturn(Math.round(rate * 100.0) / 100.0)
                .build();
    }

    @Override
    public BehaviorStatsDto getBehaviorStats(Integer userId) {
        return summarizeUserBehavior(userId);
    }

    @Override
    public List<Long> getTransactionIdsByUser(Integer userId) {
        return tradingMapper.getTransactionIdsByUser(userId);
    }
}
