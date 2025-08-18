package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.scoula.domain.mocktrading.vo.Holding;

import java.util.List;

@Mapper
public interface HoldingsMapper {

    @Select("SELECT stock_code, quantity, average_price FROM holdings WHERE account_id = #{accountId}")
    List<Holding> selectByAccountId(@Param("accountId") Integer accountId);
}
