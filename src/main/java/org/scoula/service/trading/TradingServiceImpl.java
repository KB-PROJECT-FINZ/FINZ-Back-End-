package org.scoula.service.trading;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.mapper.TradingMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradingServiceImpl implements TradingService {

    private final TradingMapper tradingMapper;

    @Override
    public List<TransactionDTO> getUserTransactions(int userId) {
        return tradingMapper.getUserTransactions(userId);
    }
}