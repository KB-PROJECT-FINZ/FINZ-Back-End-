package org.scoula.service.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.mapper.TradingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            log.warn("거래내역 없음: userId = {}", userId);

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

    @Override
    public BehaviorStatsDto getBehaviorStats(Integer userId) {
        return summarizeUserBehavior(userId); // 실제 거래 통계 생성 로직
    }

    @Override
    public List<Long> getTransactionIdsByUser(Integer userId) {
        return tradingMapper.getTransactionIdsByUser(userId); // Mapper에 정의된 쿼리 연결
    }
}