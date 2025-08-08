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

    // 전체 거래 내역 조회
    @Override
    public List<TransactionDTO> getUserTransactions(int userId) {
        List<Integer> accountIds = tradingMapper.getAccountIdsByUser(userId);
        if (accountIds == null || accountIds.isEmpty()) {
            log.warn("⚠️ userId={}에 해당하는 계좌가 없습니다.", userId);
            return List.of();
        }
        return tradingMapper.getTransactionsByAccountIds(accountIds);
    }

    // 요청된 기간 내 거래 기반 행동 분석
    @Override
    public BehaviorStatsDto getBehaviorStats(Integer userId, int periodDays) {
        List<TransactionDTO> allTx = getUserTransactions(userId);
        LocalDate cutoff = LocalDate.now().minusDays(periodDays);

        List<TransactionDTO> filtered = allTx.stream()
                .filter(tx -> tx.getExecutedAt().toLocalDate().isAfter(cutoff))
                .toList();

        log.info("📊 [기간 필터] 거래 수 ({}일): {}건", periodDays, filtered.size());

        return PortfolioStatsUtil.calculate(filtered, periodDays);
    }

    // 전체 거래 ID 조회 (계좌 기반으로 통일)
    @Override
    public List<Long> getTransactionIdsByUser(Integer userId) {
        List<TransactionDTO> transactions = getUserTransactions(userId);
        return transactions.stream()
                .map(tx -> (long) tx.getTransactionId())
                .toList();
    }

    // 요청된 기간 내 거래 ID만 필터링
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
