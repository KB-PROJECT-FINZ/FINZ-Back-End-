package org.scoula.controller.mocktrading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.mocktrading.vo.UserAccount;
import org.scoula.domain.mocktrading.vo.Holding;
import org.scoula.domain.mocktrading.vo.Transaction;
import org.scoula.service.mocktrading.UserAccountService;
import org.scoula.service.mocktrading.HoldingService;
import org.scoula.service.mocktrading.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mocktrading")
@RequiredArgsConstructor
@Log4j2
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MockTradingController {

    private final UserAccountService userAccountService;
    private final HoldingService holdingService;
    private final TransactionService transactionService;
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
     * 로그인한 사용자의 자산 현황 대시보드 조회
     * GET /api/mocktrading/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpSession session) {
        try {
            // 세션에서 로그인한 사용자 정보 가져오기
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }

            Integer userId = loginUser.getId();
            log.info("자산 현황 대시보드 조회 - 로그인 사용자 ID: {}", userId);

            // 계좌 정보 조회 또는 생성
            UserAccount account = userAccountService.getUserAccount(userId);
            if (account == null) {
                account = userAccountService.createAccountForNewUser(userId);
                if (account == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "계좌 생성에 실패했습니다."));
                }
            }

            // 보유 종목 정보 (비중 포함)
            List<Holding> holdings = holdingService.getHoldingsWithPercentage(userId, account.getTotalAssetValue());

            // 최근 거래 내역 (최대 5개)
            List<Transaction> recentTransactions = transactionService.getUserTransactions(userId, 5, 0);

            // 거래 통계 (최근 30일)
            TransactionService.TransactionSummary transactionSummary =
                    transactionService.getRecentTransactionSummary(userId, 30);

            // 대시보드 응답 구성
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("account", account);
            dashboard.put("holdings", holdings);
            dashboard.put("recentTransactions", recentTransactions);
            dashboard.put("transactionSummary", transactionSummary);

            // 포트폴리오 통계 계산
            Map<String, Object> statistics = new HashMap<>();
            long totalStockValue = holdings.stream()
                    .mapToLong(h -> h.getCurrentValue() != null ? h.getCurrentValue() : 0L)
                    .sum();

            statistics.put("totalStockValue", totalStockValue);
            statistics.put("holdingCount", holdings.size());

            // 비율 계산
            if (account.getTotalAssetValue() > 0) {
                double cashPercentage = (account.getCurrentBalance() * 100.0) / account.getTotalAssetValue();
                double stockPercentage = (totalStockValue * 100.0) / account.getTotalAssetValue();

                statistics.put("cashPercentage", Math.round(cashPercentage * 100) / 100.0);
                statistics.put("stockPercentage", Math.round(stockPercentage * 100) / 100.0);
            } else {
                statistics.put("cashPercentage", 100.0);
                statistics.put("stockPercentage", 0.0);
            }

            dashboard.put("statistics", statistics);

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("대시보드 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "대시보드 정보 조회 중 오류가 발생했습니다."));
        }
    }

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
                                                 @RequestParam(defaultValue = "50") int limit,
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
     * 주식 주문 (매수/매도)
     * POST /api/mocktrading/order
     */
    @PostMapping("/order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest, HttpSession session) {
        try {
            UserVo loginUser = (UserVo) session.getAttribute("loginUser");

            if (loginUser == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "error", "로그인이 필요합니다."));
            }

            Integer userId = loginUser.getId();
            String stockCode = (String) orderRequest.get("stockCode");
            String stockName = (String) orderRequest.get("stockName");
            String transactionType = (String) orderRequest.get("transactionType"); // BUY, SELL
            String orderType = (String) orderRequest.get("orderType"); // MARKET, LIMIT
            Integer quantity = (Integer) orderRequest.get("quantity");
            Integer price = (Integer) orderRequest.get("price");
            Integer orderPrice = (Integer) orderRequest.get("orderPrice");

            log.info("주문 요청 - 로그인 사용자 ID: {}, 종목: {}, 타입: {}, 수량: {}",
                    userId, stockCode, transactionType, quantity);

            // 필수 파라미터 검증
            if (stockCode == null || stockName == null ||
                    transactionType == null || orderType == null ||
                    quantity == null || quantity <= 0 || price == null || price <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "필수 파라미터가 누락되었습니다."));
            }

            // 주문 처리
            boolean success = transactionService.processOrder(
                    userId, stockCode, stockName, transactionType,
                    orderType, quantity, price, orderPrice);

            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "주문이 완료되었습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "주문 처리에 실패했습니다."));
            }

        } catch (Exception e) {
            log.error("주문 처리 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "주문 처리 중 오류가 발생했습니다."));
        }
    }
}