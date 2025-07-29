package org.scoula.controller.Auth;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.service.Auth.AuthService;

import org.scoula.service.Auth.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Controller
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private MailService mailService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody UserVo user, HttpSession session) {
        UserVo loginUser = authService.login(user.getUsername(), user.getPassword());
        if (loginUser != null) {
            session.setAttribute("loginUser", loginUser);
            return ResponseEntity.ok(loginUser);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 틀렸습니다.");
        }
    }

    @GetMapping("/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("로그아웃 완료");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getLoginUser(HttpSession session) {
        UserVo loginUser = (UserVo) session.getAttribute("loginUser");

        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인 상태가 아닙니다.");
        }

        String groupCode = userService.getGroupCodeByRiskType(loginUser.getRiskType());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", loginUser.getId());
        result.put("name", loginUser.getName());
        result.put("riskType", loginUser.getRiskType());
        result.put("groupCode", groupCode);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/find-username")
    @ResponseBody
    public ResponseEntity<?> findUsername(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String email = payload.get("email");

        UserVo user = authService.findByNameAndEmail(name, email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("일치하는 계정이 없습니다.");
        }
        return ResponseEntity.ok(user.getUsername());
    }

    @PostMapping("/find-password")
    @ResponseBody
    public ResponseEntity<?> findPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        UserVo user = authService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("사용자를 찾을 수 없습니다.");
        }

        // 임시 비밀번호 발급 or 재설정 링크 발송
        String tempPw = UUID.randomUUID().toString().substring(0, 8);
        authService.updatePassword(user.getId().longValue(), tempPw);
        mailService.sendTemporaryPassword(email, tempPw); // 또는 reset 링크

        return ResponseEntity.ok("메일이 발송되었습니다.");
    }
    @GetMapping("/check-nickname")
    @ResponseBody
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) {
        boolean available = userService.isNicknameAvailable(nickname);
        Map<String, Object> response = new HashMap<>();
        response.put("nickname", nickname);
        response.put("available", available);
        return ResponseEntity.ok(response);
    }
}

