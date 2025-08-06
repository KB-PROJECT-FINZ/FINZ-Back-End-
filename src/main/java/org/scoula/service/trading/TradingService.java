package org.scoula.service.trading;

import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.trading.dto.TransactionDTO;

import java.util.List;

public interface TradingService {
    List<TransactionDTO> getUserTransactions(int userId);

    BehaviorStatsDto summarizeUserBehavior(int userId); // 모의투자 행동 분석

    BehaviorStatsDto getBehaviorStats(Integer userId);

    List<Long> getTransactionIdsByUser(Integer userId);
}