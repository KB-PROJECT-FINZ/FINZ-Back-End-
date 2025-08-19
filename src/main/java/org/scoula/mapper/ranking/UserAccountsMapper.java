package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface UserAccountsMapper {

    @Select("SELECT account_id FROM user_accounts")
    List<Integer> selectAllAccountIds();

    @Select("SELECT current_balance FROM user_accounts WHERE account_id = #{accountId}")
    BigDecimal selectCurrentBalance(Integer accountId);

    @Select("SELECT initial_capital FROM user_accounts WHERE account_id = #{accountId}")
    BigDecimal selectInitialCapital(@Param("accountId") Integer accountId);
}

