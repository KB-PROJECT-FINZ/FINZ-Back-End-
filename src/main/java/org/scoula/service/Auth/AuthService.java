package org.scoula.service.Auth;

import org.scoula.domain.Auth.dto.KakaoUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.mapper.auth.AuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private AuthMapper authMapper;


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
        String email = kakaoUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("카카오에서 이메일 정보를 가져오지 못했습니다.");
        }

        String nickname = kakaoUser.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = "kakao_" + UUID.randomUUID().toString().substring(0, 8);
        }

        UserVo existingUser = authMapper.findByUsername(email);
        if (existingUser != null) {
            return existingUser;
        }

        UserVo newUser = new UserVo();
        newUser.setUsername(email);
        newUser.setNickname(nickname); // ✅ 중복 없이 한 번만 설정
        newUser.setProvider("kakao");
        newUser.setRiskType("CSD");
        newUser.setTotalCredit(0L);

        try {
            authMapper.insertUserFromKakao(newUser);
            return newUser;
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 사용자입니다.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 회원가입 실패");
        }
    }
    public UserVo findByUsername(String username) {
        return authMapper.findByUsername(username);
    }

}
