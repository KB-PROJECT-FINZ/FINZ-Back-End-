package org.scoula.mapper;

import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    int updateRiskType(@Param("username") String username, @Param("riskType") String riskType);
}
