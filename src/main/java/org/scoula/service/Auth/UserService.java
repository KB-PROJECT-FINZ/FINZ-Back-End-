package org.scoula.service.Auth;

import org.scoula.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public interface UserService {
    boolean updateRiskType(String username, String riskType);
    String getRiskTypeNameByUsername(String username);
    String getGroupCodeByRiskType(String riskType);
    boolean isNicknameAvailable(String nickname);
}
