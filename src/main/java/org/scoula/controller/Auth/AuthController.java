package org.scoula.controller.Auth;

import org.scoula.domain.Auth.dto.RegisterRequestDto;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.service.Auth.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto dto, HttpSession session) {
        if (!authService.isUsernameAvailable(dto.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 아이디입니다.");
        }

        UserVo user = UserVo.builder()
                .username(dto.getUsername())
                .password(dto.getPassword())
                .name(dto.getName())
                .nickname(dto.getNickname())
                .phoneNumber(dto.getPhoneNumber())
                .email(dto.getEmail())
                .provider("local")
                .riskType("CSD")
                .totalCredit(0L)
                .build();

        boolean success = authService.register(user);
        if (!success) {
            return ResponseEntity.status(500).body("회원가입 실패");
        }

        UserVo loggedIn = authService.login(dto.getUsername(), dto.getPassword());
        session.setAttribute("user", loggedIn);

        return ResponseEntity.ok("REGISTERED_AND_READY_FOR_PROFILE");
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpSession session) {
        UserVo user = authService.login(username, password);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
        session.setAttribute("user", user);
        return ResponseEntity.ok(user);
    }
}

