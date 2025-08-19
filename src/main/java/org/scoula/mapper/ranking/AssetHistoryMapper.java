package org.scoula.mapper.ranking;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Mapper
public interface AssetHistoryMapper {
    int insertAssetHistory(Map<String, Object> params);


}


