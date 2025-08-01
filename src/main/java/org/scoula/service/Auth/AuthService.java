package org.scoula.service.Auth;

import org.scoula.domain.Auth.dto.KakaoUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.mapper.AuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthMapper authMapper;

    public boolean isUsernameAvailable(String username) {
        return authMapper.existsByUsername(username) == 0;
    }

    public boolean isNicknameAvailable(String nickname) {
        return authMapper.existsByNickname(nickname) == 0;
    }

    public boolean register(UserVo user) {
        user.setProvider("local");
        user.setRiskType("CSD");
        user.setTotalCredit(0L);

        try {
            System.out.println(" Register 호출 전");
            int result = authMapper.insertUser(user);
            System.out.println(" Register 결과: " + result);
            return result > 0;
        } catch (Exception e) {
            System.out.println("❌ Register 중 예외 발생:");
            e.printStackTrace(); // 🔥 여기서 실제 오류 메시지 확인 가능
            return false;
        }
    }

    public UserVo login(String username, String password) {
        return authMapper.findByUsernameAndPassword(username, password);
    }

    public UserVo findByEmail(String email) {
        return authMapper.findByEmail(email);
    }

    public UserVo findByNameAndEmail(String name, String email) {
        return authMapper.findByNameAndEmail(name, email);
    }

    public boolean updatePassword(Long userId, String newPassword) {
        return authMapper.updatePassword(userId, newPassword) > 0;
    }

    public UserVo processKakaoLogin(KakaoUser kakaoUser) {
        UserVo existingUser = authMapper.findByUsername(kakaoUser.getEmail());

        if (existingUser == null) {
            UserVo newUser = new UserVo();
            newUser.setUsername(kakaoUser.getEmail());
            newUser.setNickname(kakaoUser.getNickname());
            newUser.setProvider("kakao");
            newUser.setRiskType("CSD");
            newUser.setTotalCredit(0L);

            authMapper.insertUserFromKakao(newUser);
            return newUser;
        }

        return existingUser;
    }

}
