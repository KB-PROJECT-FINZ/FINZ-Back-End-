package org.scoula.service.trading;

import org.scoula.domain.trading.dto.TransactionDTO;

import java.util.List;

public interface TradingService {
    List<TransactionDTO> getUserTransactions(int userId);
}