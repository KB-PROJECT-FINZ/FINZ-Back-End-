package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.api.mocktrading.ConditionSearchApi;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.mocktrading.vo.UserAccount;
import org.scoula.domain.mocktrading.vo.Holding;
import org.scoula.domain.mocktrading.vo.Transaction;
import org.scoula.service.mocktrading.UserAccountService;
import org.scoula.service.mocktrading.HoldingService;
import org.scoula.service.mocktrading.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mocktrading")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
public class MockTradingController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DataSource dataSource;

    private final UserAccountService userAccountService;
    private final HoldingService holdingService;

    /**
     * 로그인한 사용자의 계좌 정보 조회
     * GET /api/mocktrading/account
     */
    @GetMapping("/account")
    public ResponseEntity<?> getUserAccount(HttpSession session) {
        try {
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }

            Integer userId = loginUser.getId();
            log.info("사용자 계좌 정보 조회 - 로그인 사용자 ID: {}", userId);

            UserAccount account = userAccountService.getUserAccount(userId);
            if (account == null) {
                account = userAccountService.createAccountForNewUser(userId);
                if (account == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "계좌 생성에 실패했습니다."));
                }
            }

            return ResponseEntity.ok(account);

        } catch (Exception e) {
            log.error("계좌 정보 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "계좌 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그인한 사용자의 보유 종목 조회
     * GET /api/mocktrading/holdings
     */
    @GetMapping("/holdings")
    public ResponseEntity<?> getUserHoldings(HttpSession session) {
        try {
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }

            Integer userId = loginUser.getId();
            log.info("보유 종목 조회 - 로그인 사용자 ID: {}", userId);

            List<Holding> holdings = holdingService.getUserHoldings(userId);
            return ResponseEntity.ok(holdings);

        } catch (Exception e) {
            log.error("보유 종목 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "보유 종목 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그인한 사용자의 거래 내역 조회
     * GET /api/mocktrading/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getUserTransactions(HttpSession session,
                                                 @RequestParam(defaultValue = "1000") int limit,
                                                 @RequestParam(defaultValue = "0") int offset) {
        try {
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }

            Integer userId = loginUser.getId();
            log.info("거래 내역 조회 - 로그인 사용자 ID: {}, limit: {}, offset: {}", userId, limit, offset);

            List<Transaction> transactions = transactionService.getUserTransactions(userId, limit, offset);
            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            log.error("거래 내역 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "거래 내역 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 사용자 크레딧 조회
     * GET /api/mocktrading/user/credit
     */
    @GetMapping("/user/credit")
    public ResponseEntity<?> getUserCredit(HttpSession session) {
        try {
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");
            if (loginUser == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }

            Integer userId = loginUser.getId();
            Integer totalCredit = userAccountService.getUserCredit(userId);

            return ResponseEntity.ok(Map.of("totalCredit", totalCredit));
        } catch (Exception e) {
            log.error("크레딧 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "크레딧 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 크레딧 충전 (포인트를 현금으로 전환)
     * POST /api/mocktrading/charge-credit
     */
    @PostMapping("/charge-credit")
    public ResponseEntity<?> chargeCredit(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "error", "로그인이 필요합니다."));
            }

            Integer userId = loginUser.getId();
            Integer creditAmount = (Integer) request.get("creditAmount");

            log.info("크레딧 충전 요청 - 로그인 사용자 ID: {}, 충전 크레딧: {}", userId, creditAmount);

            if (creditAmount == null || creditAmount <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "유효하지 않은 충전 금액입니다."));
            }

            boolean success = userAccountService.chargeCredit(userId, creditAmount);

            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "충전이 완료되었습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "크레딧이 부족하거나 충전에 실패했습니다."));
            }

        } catch (Exception e) {
            log.error("크레딧 충전 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "충전 중 오류가 발생했습니다."));
        }
    }

    /**
     * 종목조건검색 목록조회 테스트 - GET 방식
     * 이미지 URL 추가 로직 포함
     *
     * @param userId 사용자 ID (선택, 기본값: "tls12251")
     * @param seq 시퀀스 번호 (선택, 기본값: "0")
     * @return 조회 결과 (이미지 URL 포함)
     */
    @GetMapping("/condition-search")
    public ResponseEntity<Map<String, Object>> getConditionSearchResult(
            @RequestParam(value = "user_id", defaultValue = "tls12251") String userId,
            @RequestParam(value = "seq", defaultValue = "0") String seq) {

        Map<String, Object> response = new HashMap<>();

        try {
            // API 호출
            JsonNode result = ConditionSearchApi.getConditionSearchResult(userId, seq);

            // 이미지 URL 추가 처리
            JsonNode enhancedResult = addImageUrlsToConditionSearchResult(result);

            response.put("success", true);
            response.put("message", "종목조건검색 목록조회 성공");
            response.put("data", enhancedResult);
            response.put("userId", userId);
            response.put("seq", seq);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "API 호출 실패: " + e.getMessage());
            response.put("userId", userId);
            response.put("seq", seq);
            response.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "예상치 못한 오류: " + e.getMessage());
            response.put("userId", userId);
            response.put("seq", seq);
            response.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Condition Search 결과에 이미지 URL 추가
     */
    private JsonNode addImageUrlsToConditionSearchResult(JsonNode originalResult) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode result = mapper.readTree(mapper.writeValueAsString(originalResult));

            JsonNode output2 = result.path("output2");
            if (output2.isArray()) {
                ArrayNode output2Array = (ArrayNode) output2;

                for (int i = 0; i < output2Array.size(); i++) {
                    JsonNode stock = output2Array.get(i);
                    if (stock.isObject()) {
                        ObjectNode stockObj = (ObjectNode) stock;

                        // 종목 코드 추출 (여러 필드명 시도)
                        String stockCode = getFieldValue(stock, "mksc_shrn_iscd", "stck_shrn_iscd", "code");

                        if (!stockCode.isEmpty()) {
                            // DB에서 이미지 URL 조회
                            String imageUrl = getStockImageUrl(stockCode);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                stockObj.put("imageUrl", imageUrl);
                            }
                        }
                    }
                }
            }

            return result;

        } catch (Exception e) {
            log.error("이미지 URL 추가 중 오류 발생", e);
            return originalResult; // 오류 시 원본 반환
        }
    }

    /**
     * DB에서 종목 코드로 이미지 URL 조회
     * VolumeRankingApi의 getStockImageUrl 메서드와 동일
     */
    private String getStockImageUrl(String stockCode) {
        String sql = "SELECT image_url FROM stocks WHERE code = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stockCode);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("image_url");
            }

        } catch (Exception e) {
            log.error("이미지 URL 조회 실패: {} - {}", stockCode, e.getMessage());
        }

        return null;
    }

    /**
     * 여러 필드명을 시도해서 값을 가져오는 헬퍼 메서드
     * VolumeRankingApi의 getFieldValue 메서드와 동일
     */
    private String getFieldValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.asText().isEmpty() && !"0".equals(field.asText())) {
                return field.asText();
            }
        }
        return "";
    }
}