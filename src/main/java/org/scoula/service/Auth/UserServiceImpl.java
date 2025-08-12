package org.scoula.service.Auth;

import org.apache.ibatis.annotations.Param;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.type.dto.RiskTypeDto;
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

    @Override
    public RiskTypeDto findRiskTypeByRiskType(String riskType){
        return userMapper.findRiskTypeByRiskType(riskType);
    };

    @Override
    public boolean isEmailAvailable(String email) {
        return userMapper.countByEmail(email) == 0;
    }

    @Override
    public boolean updateProfileImage(Integer userId, Integer profileImage) {
        try {
            int result = userMapper.updateProfileImage(userId, profileImage);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UserVo findById(Integer userId) {
        UserVo user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId);
        }
        return user;
    }

    @Override
    public boolean updateNickname(Integer userId, String nickname) {
        int result = userMapper.updateNickname(userId, nickname);
        return result > 0;
    }
}
