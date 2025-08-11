package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.trading.dto.TransactionDTO;
import java.util.List;

@Mapper
public interface TradingMapper {
    List<Integer> getAccountIdsByUser(@Param("userId") int userId);

    List<TransactionDTO> getTransactionsByAccountIds(@Param("accountIds") List<Integer> accountIds);
}