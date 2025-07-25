package org.scoula.service.Auth;

import org.scoula.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public boolean updateRiskType(String username, String riskType) {
        return userMapper.updateRiskType(username, riskType) > 0;
    }
    public String getRiskTypeNameByUsername(String username) {
        return userMapper.findRiskTypeNameByUsername(username);
    }
}
