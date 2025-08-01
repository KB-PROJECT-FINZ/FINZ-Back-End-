package org.scoula.service.trading;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.mapper.TradingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public abstract class TradingServiceImpl implements TradingService {

    private final TradingMapper tradingMapper;

    @Override
    public List<TransactionDTO> getUserTransactions(int userId) {
        return tradingMapper.getUserTransactions(userId);
    }

    @Override
    public BehaviorStatsDto summarizeUserBehavior(int userId) {
        List<TransactionDTO> transactions = tradingMapper.getUserTransactions(userId);

        if (transactions.isEmpty()) {
            return BehaviorStatsDto.builder()
                    .transactionCount(0)
                    .analysisPeriod(0)
                    .totalReturn(0.0)
                    .startDate(null)
                    .endDate(null)
                    .build();
        }

        TransactionDTO first = transactions.get(transactions.size() - 1);
        TransactionDTO last = transactions.get(0);

        long days = ChronoUnit.DAYS.between(
                LocalDateTime.parse(first.getExecutedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                LocalDateTime.parse(last.getExecutedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        double returnSum = transactions.stream()
                .mapToDouble(t -> {
                    double diff = t.getTransactionType().equals("BUY") ? -1 : 1;
                    return diff * t.getQuantity() * t.getPrice();
                })
                .sum();

        return BehaviorStatsDto.builder()
                .transactionCount(transactions.size())
                .analysisPeriod((int) days)
                .startDate(first.getExecutedAt().substring(0, 10))
                .endDate(last.getExecutedAt().substring(0, 10))
                .totalReturn(returnSum)
                .build();
    }
}

