package org.scoula.service.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.mapper.TradingMapper;
import org.scoula.util.chatbot.PortfolioStatsUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        return PortfolioStatsUtil.calculateWithQuantity(transactions); // ✅ 수량 기반 계산
    }

    @Override
    public BehaviorStatsDto getBehaviorStats(Integer userId) {
        return summarizeUserBehavior(userId);
    }

    @Override
    public BehaviorStatsDto getBehaviorStats(Integer userId, int periodDays) {
        List<TransactionDTO> allTx = getUserTransactions(userId);
        LocalDate cutoff = LocalDate.now().minusDays(periodDays);

        List<TransactionDTO> filtered = allTx.stream()
                .filter(tx -> tx.getExecutedAt().toLocalDate().isAfter(cutoff))
                .toList();

        log.info("📊 [기간 필터] 거래 수 ({}일): {}건", periodDays, filtered.size());

        return PortfolioStatsUtil.calculateWithQuantity(filtered); // ✅ 수량 기반 계산
    }

    @Override
    public List<Long> getTransactionIdsByUser(Integer userId) {
        return tradingMapper.getTransactionIdsByUser(userId);
    }

    @Override
    public List<Long> getTransactionIdsByUser(Integer userId, int periodDays) {
        List<TransactionDTO> allTx = getUserTransactions(userId);
        LocalDate cutoff = LocalDate.now().minusDays(periodDays);

        return allTx.stream()
                .filter(tx -> tx.getExecutedAt().toLocalDate().isAfter(cutoff))
                .map(tx -> (long) tx.getTransactionId())
                .toList();
    }
}
