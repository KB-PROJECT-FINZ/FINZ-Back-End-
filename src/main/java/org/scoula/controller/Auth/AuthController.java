package org.scoula.controller.Auth;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.service.Auth.AuthService;

import org.scoula.service.Auth.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/auth")
public class AuthController {

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
}

