package org.scoula.controller.Auth;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.service.Auth.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthService authService;

    //  회원가입
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserVo user) {
        boolean result = authService.register(user);
        if (result) {
            return ResponseEntity.ok("회원가입 성공");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("회원가입 실패");
        }
    }

    //  로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        UserVo user = authService.login(username, password);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam("nickname") String nickname) {
        System.out.println("📌 닉네임 중복확인 요청 nickname: " + nickname);
        boolean available = authService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(Map.of("available", available));
    }

    //  아이디 중복확인
    @GetMapping("/check")
    public ResponseEntity<?> checkUsername(@RequestParam("username") String username) {
        boolean available = authService.isUsernameAvailable(username);
        return ResponseEntity.ok(Map.of("available", available));
    }
}

