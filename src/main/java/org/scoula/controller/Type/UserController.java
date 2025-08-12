package org.scoula.controller.Type;

import lombok.extern.log4j.Log4j2;
import org.scoula.config.auth.LoginUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.learning.dto.LearningQuizDTO;
import org.scoula.domain.type.dto.RiskTypeDto;
import org.scoula.domain.type.dto.RiskTypeUpdateDto;
import org.scoula.service.Auth.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Log4j2
public class UserController {

    @Autowired
    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/risk_type")
    public ResponseEntity<?> updateRiskType(@RequestBody RiskTypeUpdateDto dto, HttpSession session) {
        System.out.println(" POST /api/user/risk_type 호출됨");
        System.out.println("username: " + dto.getUsername());
        System.out.println("riskType: " + dto.getRiskType());

        boolean success = userService.updateRiskType(dto.getUsername(), dto.getRiskType());

        if (success) {
            //  최신 정보로 세션 전체 객체를 덮어쓰기
            UserVo refreshed = userService.findByUsername(dto.getUsername());
            if (refreshed != null) {
                session.setAttribute("loginUser", refreshed); // 완전히 새 객체로 갱신
                System.out.println("세션 riskType 완전 갱신됨: " + refreshed.getRiskType());
            }
            return ResponseEntity.ok("성향 저장 성공 + 세션 전체 갱신");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("저장 실패");
        }
    }

    @GetMapping("/risk-type-name")
    public ResponseEntity<String> getRiskTypeName(@RequestParam String username) {
        String nameKr = userService.getRiskTypeNameByUsername(username);
        return nameKr != null
                ? ResponseEntity.ok(nameKr)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 사용자");
    }

    @GetMapping("/risk-type-detail/{code}")
    public ResponseEntity<RiskTypeDto> getRiskTypeDetail(@PathVariable("code") String riskType) {
        return ResponseEntity.ok(userService.findRiskTypeByRiskType(riskType));
    }

    /**
     * 프로필 이미지 선택 업데이트 (assets 이미지 번호 방식)
     * 1: finz.png (기본)
     * 2: FINZ_고양이.png
     * 3: FINZ_곰.png
     * 4: FINZ_병아리.png
     * 5: FINZ_원숭이.png
     * 6: FINZ_코끼리.png
     * 7: FINZ_토끼.png
     */
    @PostMapping("/update-profile-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfileImage(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        try {
            HttpSession session = request.getSession();
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null || loginUser.getId() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "로그인이 필요합니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 프로필 이미지 번호 추출
            Object profileImageObj = requestBody.get("profile_image");
            Integer profileImageNumber;

            try {
                if (profileImageObj instanceof Integer) {
                    profileImageNumber = (Integer) profileImageObj;
                } else if (profileImageObj instanceof String) {
                    profileImageNumber = Integer.parseInt((String) profileImageObj);
                } else {
                    throw new NumberFormatException("Invalid profile_image type");
                }
            } catch (NumberFormatException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "유효하지 않은 이미지 번호입니다");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 유효성 검사 (1-7)
            if (profileImageNumber < 1 || profileImageNumber > 7) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "이미지 번호는 1-7 사이여야 합니다");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            boolean success = userService.updateProfileImage(loginUser.getId(), profileImageNumber);

            if (!success) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "프로필 이미지 업데이트에 실패했습니다");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            // 세션의 사용자 정보도 업데이트
            loginUser.setProfileImage(profileImageNumber);
            session.setAttribute("loginUser", loginUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("profile_image", profileImageNumber);
            response.put("message", "프로필 이미지가 성공적으로 변경되었습니다");

            log.info("프로필 이미지 변경 성공 - 사용자 ID: {}, 이미지 번호: {}",
                    loginUser.getId(), profileImageNumber);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("프로필 이미지 변경 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "프로필 이미지 변경 중 오류가 발생했습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/update-nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null) {
                response.put("error", "로그인이 필요합니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String newNickname = requestBody.get("nickname");

            if (newNickname == null || newNickname.trim().isEmpty()) {
                response.put("error", "닉네임을 입력해주세요");
                return ResponseEntity.badRequest().body(response);
            }

            if (newNickname.trim().length() < 2 || newNickname.trim().length() > 10) {
                response.put("error", "닉네임은 2-10자로 입력해주세요");
                return ResponseEntity.badRequest().body(response);
            }

            if (newNickname.trim().equals(loginUser.getNickname())) {
                response.put("error", "현재 닉네임과 동일합니다");
                return ResponseEntity.badRequest().body(response);
            }

            if (!userService.isNicknameAvailable(newNickname.trim())) {
                response.put("error", "이미 사용 중인 닉네임입니다");
                return ResponseEntity.badRequest().body(response);
            }

            boolean success = userService.updateNickname(loginUser.getId(), newNickname.trim());

            if (success) {
                UserVo refreshedUser = userService.findById(loginUser.getId());
                session.setAttribute("loginUser", refreshedUser);

                response.put("message", "닉네임이 성공적으로 변경되었습니다");
                response.put("nickname", newNickname.trim());

                log.info("닉네임 변경 성공 - 사용자 ID: {}, 새 닉네임: {}",
                        loginUser.getId(), newNickname.trim());

                return ResponseEntity.ok(response);
            } else {
                response.put("error", "닉네임 변경에 실패했습니다");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            log.error("닉네임 변경 중 오류 발생", e);
            response.put("error", "닉네임 변경 중 오류가 발생했습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}