package org.scoula.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.domain.Auth.vo.UserVo;

public interface UserMapper {
    String findRiskTypeNameByUsername(String username);
    int updateRiskType(@Param("username") String username, @Param("riskType") String riskType);
    int countByNickname(@Param("nickname") String nickname);
    String findGroupCodeByRiskType(String riskType);
    UserVo findByUsername(@Param("username") String username);

}
