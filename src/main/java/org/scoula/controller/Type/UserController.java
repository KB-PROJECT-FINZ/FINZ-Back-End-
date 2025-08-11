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
    @PostMapping("/upload-profile-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("image") MultipartFile file,
            HttpServletRequest request) {

        try {
            // 세션에서 사용자 정보 가져오기
            HttpSession session = request.getSession();
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null || loginUser.getId() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "로그인이 필요합니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 파일 검증
            if (file.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "파일을 선택해주세요");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 파일 크기 검증 (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "파일 크기는 5MB 이하여야 합니다");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 파일 타입 검증
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "이미지 파일만 업로드 가능합니다");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 업로드 디렉토리 설정
            String uploadDir = "uploads/profile/";
            Path uploadPath = Paths.get(uploadDir);

            // 디렉토리가 없으면 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 고유 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = loginUser.getId() + "_" + System.currentTimeMillis() + fileExtension;

            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 이전 프로필 이미지 삭제 (선택적)
            UserVo currentUser = userService.findById(loginUser.getId());
            if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                try {
                    // 기존 파일 경로에서 파일명 추출
                    String oldImagePath = currentUser.getProfileImage();
                    if (oldImagePath.startsWith("/uploads/")) {
                        String oldFileName = oldImagePath.substring("/uploads/".length());
                        Path oldFilePath = Paths.get("uploads/" + oldFileName);
                        Files.deleteIfExists(oldFilePath);
                        log.info("이전 프로필 이미지 삭제: " + oldFilePath);
                    }
                } catch (Exception e) {
                    // 이전 파일 삭제 실패는 로그만 남기고 계속 진행
                    log.warn("이전 프로필 이미지 삭제 실패: " + e.getMessage());
                }
            }

            // 데이터베이스 업데이트
            String imageUrl = "/uploads/profile/" + fileName;
            userService.updateProfileImage(loginUser.getId(), imageUrl);

            // 세션의 사용자 정보도 업데이트
            loginUser.setProfileImage(imageUrl);
            session.setAttribute("loginUser", loginUser);

            Map<String, Object> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "프로필 이미지가 성공적으로 업데이트되었습니다");

            log.info("프로필 이미지 업로드 성공 - 사용자 ID: " + loginUser.getId() + ", 파일명: " + fileName);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "파일 저장 중 오류가 발생했습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "프로필 이미지 업로드 중 오류가 발생했습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    @PostMapping("/reset-profile-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetProfileImage(HttpServletRequest request) {

        try {
            // 세션에서 사용자 정보 가져오기
            HttpSession session = request.getSession();
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null || loginUser.getId() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "로그인이 필요합니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 현재 사용자 정보 가져오기
            UserVo currentUser = userService.findById(loginUser.getId());

            // 기존 프로필 이미지 파일 삭제
            if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                try {
                    String oldImagePath = currentUser.getProfileImage();
                    if (oldImagePath.startsWith("/uploads/")) {
                        String oldFileName = oldImagePath.substring("/uploads/".length());
                        Path oldFilePath = Paths.get("uploads/" + oldFileName);
                        Files.deleteIfExists(oldFilePath);
                        log.info("기존 프로필 이미지 삭제: " + oldFilePath);
                    }
                } catch (Exception e) {
                    log.warn("기존 프로필 이미지 삭제 실패: " + e.getMessage());
                }
            }

            // 🔥 중요: 매개변수 이름을 profileImage로 맞춤
            userService.updateProfileImage(loginUser.getId(), null);

            // 세션의 사용자 정보도 업데이트
            loginUser.setProfileImage(null);
            session.setAttribute("loginUser", loginUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "프로필 이미지가 기본 이미지로 변경되었습니다");
            response.put("imageUrl", null);

            log.info("프로필 이미지 초기화 완료 - 사용자 ID: " + loginUser.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("프로필 이미지 초기화 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "프로필 이미지 초기화 중 오류가 발생했습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    @PostMapping("/update-nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 세션에서 사용자 정보 가져오기
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null) {
                response.put("error", "로그인이 필요합니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String newNickname = requestBody.get("nickname");

            // 닉네임 유효성 검사
            if (newNickname == null || newNickname.trim().isEmpty()) {
                response.put("error", "닉네임을 입력해주세요");
                return ResponseEntity.badRequest().body(response);
            }

            // 닉네임 길이 검사 (2-10자)
            if (newNickname.trim().length() < 2 || newNickname.trim().length() > 10) {
                response.put("error", "닉네임은 2-10자로 입력해주세요");
                return ResponseEntity.badRequest().body(response);
            }

            // 현재 닉네임과 동일한지 확인
            if (newNickname.trim().equals(loginUser.getNickname())) {
                response.put("error", "현재 닉네임과 동일합니다");
                return ResponseEntity.badRequest().body(response);
            }

            // 닉네임 중복 검사
            if (!userService.isNicknameAvailable(newNickname.trim())) {
                response.put("error", "이미 사용 중인 닉네임입니다");
                return ResponseEntity.badRequest().body(response);
            }

            // 닉네임 업데이트
            boolean success = userService.updateNickname(loginUser.getId(), newNickname.trim());

            if (success) {
                // 세션의 사용자 정보 갱신
                UserVo refreshedUser = userService.findById(loginUser.getId());
                session.setAttribute("loginUser", refreshedUser);

                response.put("message", "닉네임이 성공적으로 변경되었습니다");
                response.put("nickname", newNickname.trim());

                log.info("닉네임 변경 성공 - 사용자 ID: {}, 새 닉네임: {}", loginUser.getId(), newNickname.trim());

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