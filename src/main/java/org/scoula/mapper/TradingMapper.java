package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.trading.dto.TransactionDTO;
import java.util.List;

@Mapper
public interface TradingMapper {
    List<TransactionDTO> getUserTransactions(@Param("userId") int userId);
}