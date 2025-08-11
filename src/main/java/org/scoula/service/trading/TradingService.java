package org.scoula.service.trading;

import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;

import java.util.List;

public interface TradingService {
    List<TransactionDTO> getUserTransactions(int userId);

    BehaviorStatsDto getBehaviorStats(Integer userId, int periodDays); // ✅ 추가

    List<Long> getTransactionIdsByUser(Integer userId); // 기존

    List<Long> getTransactionIdsByUser(Integer userId, int periodDays); // ✅ 추가
}