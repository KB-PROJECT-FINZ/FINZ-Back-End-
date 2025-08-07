package org.scoula.controller.Type;

import org.scoula.config.auth.LoginUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.learning.dto.LearningQuizDTO;
import org.scoula.domain.type.dto.RiskTypeDto;
import org.scoula.domain.type.dto.RiskTypeUpdateDto;
import org.scoula.service.Auth.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

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

}
