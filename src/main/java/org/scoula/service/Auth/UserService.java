package org.scoula.service.Auth;

import org.apache.ibatis.annotations.Param;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.type.dto.RiskTypeDto;
import org.scoula.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public interface UserService {
    boolean updateRiskType(String username, String riskType);
    String getRiskTypeNameByUsername(String username);
    String getGroupCodeByRiskType(String riskType);
    boolean isNicknameAvailable(String nickname);
    UserVo findByUsername(String username);
    String getRiskTypeByUserId(Integer userId);
    RiskTypeDto findRiskTypeByRiskType(String riskType);
    boolean isEmailAvailable(String email);

    UserVo findById(Integer userId);
    boolean updateProfileImage(Integer userId, Integer profileImage);

    boolean updateNickname(Integer userId, String nickname);
}
