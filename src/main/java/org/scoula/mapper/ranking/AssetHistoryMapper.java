package org.scoula.mapper.ranking;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.math.BigDecimal;
import java.time.LocalDate;

@Mapper
public interface AssetHistoryMapper {

     void insertAssetHistory(
            @Param("accountId") Integer accountId,
            @Param("baseDate") LocalDate baseDate,
            @Param("totalAssetValue") BigDecimal totalAssetValue,
            @Param("cashBalance") BigDecimal cashBalance,
            @Param("stockValue") BigDecimal stockValue,
            @Param("profitLoss") BigDecimal profitLoss,
            @Param("realizedProfitLoss") BigDecimal realizedProfitLoss,
            @Param("profitRate") BigDecimal profitRate
    );
}


