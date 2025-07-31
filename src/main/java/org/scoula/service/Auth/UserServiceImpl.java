package org.scoula.service.Auth;

import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean updateRiskType(String username, String riskType) {
        return userMapper.updateRiskType(username, riskType) > 0;
    }

    @Override
    public String getRiskTypeNameByUsername(String username) {
        return userMapper.findRiskTypeNameByUsername(username);
    }

    @Override
    public String getGroupCodeByRiskType(String riskType) {
        return userMapper.findGroupCodeByRiskType(riskType);
    }

    @Override
    public boolean isNicknameAvailable(String nickname) {
        return userMapper.countByNickname(nickname) == 0;
    }

    @Override
    public UserVo findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    @Override
    public String getRiskTypeByUserId(Integer userId) {
        return userMapper.findRiskTypeByUserId(userId);
    }
}