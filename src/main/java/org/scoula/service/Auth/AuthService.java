package org.scoula.service.Auth;


import org.scoula.mapper.AuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.scoula.domain.Auth.vo.UserVo;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthMapper authMapper;

    public boolean isUsernameAvailable(String username) {
        return authMapper.existsByUsername(username) == 0;
    }

    public boolean register(UserVo user) {
        user.setProvider("local");
        user.setRiskType("CSD");
        user.setTotalCredit(0L);
        return authMapper.insertUser(user) > 0;
    }

    public UserVo login(String username, String password) {
        return authMapper.findByUsernameAndPassword(username, password);
    }
}