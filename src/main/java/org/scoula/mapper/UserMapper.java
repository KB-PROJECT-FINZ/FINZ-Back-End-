package org.scoula.mapper;

import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    String findRiskTypeNameByUsername(String username);
    int updateRiskType(@Param("username") String username, @Param("riskType") String riskType);

    String findGroupCodeByRiskType(String riskType);
}
