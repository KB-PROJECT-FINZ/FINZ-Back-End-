package org.scoula.controller.Auth;

import org.scoula.domain.Auth.dto.RegisterRequestDto;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
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
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto dto) {
        if (!authService.isUsernameAvailable(dto.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 아이디입니다.");
        }

        UserVo user = new UserVo();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setName(dto.getName());
        user.setNickname(dto.getNickname());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setEmail(dto.getEmail());

        boolean success = authService.register(user);
        return success ? ResponseEntity.ok("회원가입 성공") : ResponseEntity.status(500).body("회원가입 실패");
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpSession session) {
        User user = authService.login(username, password);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
        session.setAttribute("user", user);
        return ResponseEntity.ok(user);
    }
}
