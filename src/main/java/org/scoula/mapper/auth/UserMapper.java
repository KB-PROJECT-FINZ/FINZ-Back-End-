package org.scoula.mapper.auth;

import org.apache.ibatis.annotations.Param;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.type.dto.RiskTypeDto;

public interface UserMapper {
    String findRiskTypeNameByUsername(String username);
    int updateRiskType(@Param("username") String username, @Param("riskType") String riskType);
    String findUsernameByUserId(@Param("userId") Integer userId);
    int countByNickname(@Param("nickname") String nickname);
    String findGroupCodeByRiskType(String riskType);
    UserVo findByUsername(@Param("username") String username);

    String findRiskTypeByUsername(@Param("username") String username);
    String findRiskTypeByUserId(@Param("userId") Integer userId);
    int countByEmail(@Param("email") String email);
    RiskTypeDto findRiskTypeByRiskType(@Param("riskType") String riskType);

    int updateProfileImage(@Param("userId") Integer userId, @Param("profileImage") Integer profileImage);
    UserVo findById(@Param("userId") Integer userId);

    int updateNickname(@Param("userId") Integer userId, @Param("nickname") String nickname);
}
