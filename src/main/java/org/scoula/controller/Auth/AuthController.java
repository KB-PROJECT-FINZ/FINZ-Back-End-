package org.scoula.controller.Auth;
import org.scoula.domain.Auth.dto.KakaoUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.service.Auth.AuthService;
import org.scoula.service.Auth.KakaoAuthService;
import org.scoula.service.mocktrading.UserAccountService;
import org.scoula.domain.mocktrading.vo.UserAccount;
import org.scoula.service.Auth.MailService;
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
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private MailService mailService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private KakaoAuthService kakaoAuthService;
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

    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<?> signup(@RequestBody UserVo user, HttpSession session) {
        System.out.println("🔥 POST 요청 들어옴: " + user.getEmail());
        boolean result = authService.register(user);
        if (result) {
            try {
                UserVo registeredUser = authService.findByEmail(user.getEmail());
                session.setAttribute("loginUser", registeredUser);
                if (registeredUser != null && registeredUser.getId() != null) {
                    System.out.println("등록된 사용자 ID: " + registeredUser.getId());

                    UserAccount userAccount = userAccountService.createAccountForNewUser(registeredUser.getId());
                    if (userAccount != null) {
                        System.out.println("계좌 생성 성공 - 사용자 ID: " + registeredUser.getId() + ", 계좌번호: " + userAccount.getAccountNumber());

                        Map<String, Object> response = new HashMap<>();

                        response.put("message", "회원가입 성공");
                        response.put("accountCreated", true);
                        response.put("accountNumber", userAccount.getAccountNumber());
                        response.put("initialBalance", userAccount.getCurrentBalance());
                        response.put("userId", registeredUser.getId());
                        return ResponseEntity.ok(response);
                    } else {
                        System.out.println("⚠계좌 생성 실패 - 사용자 ID: " + registeredUser.getId() + " (회원가입은 성공)");
                    }
                } else {
                    System.out.println("등록된 사용자 정보 조회 실패 - 이메일: " + user.getEmail());
                }
            } catch (Exception e) {
                System.out.println("계좌 생성 중 오류: " + e.getMessage() + " (회원가입은 성공)");
                e.printStackTrace();
            }
            return ResponseEntity.ok("회원가입 성공");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("회원가입 실패");
        }
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
        result.put("username", loginUser.getUsername());
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
        try {
            String email = body.get("email");

            System.out.println("🔍 요청 email: " + email);

            UserVo user = authService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("사용자를 찾을 수 없습니다.");
            }

            String tempPw = UUID.randomUUID().toString().substring(0, 8);
            System.out.println(" 임시 비번 생성: " + tempPw);

            authService.updatePassword(user.getId().longValue(), tempPw);
            System.out.println(" 비밀번호 업데이트 성공");

            mailService.sendTemporaryPassword(email, tempPw);
            System.out.println(" 메일 발송 성공");

            return ResponseEntity.ok("메일이 발송되었습니다.");

        } catch (Exception e) {
            e.printStackTrace(); // 서버 콘솔에 전체 예외 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류 발생");
        }
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

    @RequestMapping("/auth/kakao/callback")
    public String kakaoCallback(@RequestParam String code, HttpSession session) {
        try {
            String accessToken = kakaoAuthService.getAccessToken(code);
            KakaoUser kakaoUser = kakaoAuthService.getKakaoUserInfo(accessToken);
            UserVo user = authService.processKakaoLogin(kakaoUser);

            // 세션에 저장
            session.setAttribute("username", user.getUsername());
            session.setAttribute("name", user.getNickname());

            return "redirect:/"; // ✅ 여기 중요! -> 홈으로 이동
        } catch (Exception e) {
            e.printStackTrace(); // 오류 로그 출력
            return "redirect:/login-error"; // 또는 오류 처리 페이지
        }
    }
}

