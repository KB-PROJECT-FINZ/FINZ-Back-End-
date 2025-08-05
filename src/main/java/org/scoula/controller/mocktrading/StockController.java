package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.api.mocktrading.PriceApi;
import org.scoula.api.mocktrading.RealtimeBidsAndAsksClient;
import org.scoula.api.mocktrading.RealtimeExecutionClient;
import org.scoula.domain.mocktrading.MarketOrderRequestDto;
import org.scoula.domain.mocktrading.OrderRequestDto;
import org.scoula.util.mocktrading.ConfigManager;
import org.scoula.service.mocktrading.StockIndustryUpdaterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
@Api(tags = "주식 모의투자 API", description = "주식 가격 조회 및 모의 매매 기능을 제공합니다")
public class StockController {


    // 주식 기본정보api -> stocks 업종 업데이트
    private final StockIndustryUpdaterService stockIndustryUpdaterService;

    @PostMapping("/update-industries")
    @ApiOperation(
            value = "모든 종목의 업종 정보 업데이트",
            notes = "stocks 테이블의 모든 종목 코드에 대해 업종 정보를 API로부터 조회하고 DB에 반영합니다"
    )
    public ResponseEntity<String> updateIndustries() {
        stockIndustryUpdaterService.updateAllStockIndustries();
        return ResponseEntity.ok("✅ 업종 정보 일괄 업데이트 완료");
    }
    @GetMapping("/prices/{stockCodes}")
    @ApiOperation(value = "다중 주식 가격 조회", notes = "여러 주식 종목코드를 콤마로 구분하여 한번에 실시간 가격 정보를 조회합니다")
    @ApiParam(name = "stockCodes", value = "주식 종목코드들을 콤마로 구분 (예: 005930,000660,035420)", required = true, example = "005930,000660,035420")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공적으로 가격 정보들을 조회했습니다"),
            @ApiResponse(code = 400, message = "잘못된 요청입니다 (종목코드 형식 오류)"),
            @ApiResponse(code = 500, message = "서버 내부 오류가 발생했습니다")
    })
    public ResponseEntity<Map<String, Object>> getMultipleStockPrices(@PathVariable("stockCodes") String stockCodes) {
        try {
            // 콤마로 구분된 종목코드들을 배열로 분리
            String[] codes = stockCodes.split(",");

            // 결과를 담을 Map 생성
            Map<String, Object> result = new HashMap<>();
            Map<String, JsonNode> prices = new HashMap<>();
            List<String> errors = new ArrayList<>();

            // 각 종목코드에 대해 가격 조회
            for (String code : codes) {
                String trimmedCode = code.trim();

                // 종목코드 유효성 검사 (6자리 숫자)
                if (!trimmedCode.matches("\\d{6}")) {
                    errors.add(trimmedCode + ": 올바르지 않은 종목코드 형식");
                    continue;
                }

                try {
                    JsonNode priceData = PriceApi.getPriceData(trimmedCode);
                    prices.put(trimmedCode, priceData);
                } catch (IOException e) {
                    errors.add(trimmedCode + ": 가격 조회 실패 - " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("data", prices);
            result.put("requestedCount", codes.length);
            result.put("successCount", prices.size());
            result.put("errorCount", errors.size());

            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "서버 내부 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    @GetMapping("/price/{stockCode}")
    @ApiOperation(value = "주식 가격 조회", notes = "주식 종목코드를 이용하여 실시간 가격 정보를 조회합니다")
    @ApiParam(name = "code", value = "주식 종목코드 (예: 005930)", required = true, example = "005930")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공적으로 가격 정보를 조회했습니다"),
            @ApiResponse(code = 500, message = "서버 내부 오류가 발생했습니다")
    })
    public ResponseEntity<JsonNode> getStockPrice(@PathVariable("stockCode") String code) {
        try {
            JsonNode price = PriceApi.getPriceData(code);
            return ResponseEntity.ok(price);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/trading")
    @ApiOperation(
            value = "트레이딩 페이지 접근 시 실시간 웹소켓(호가+체결) 시작",
            notes = "stockCode 파라미터를 받아 해당 종목의 실시간 호가 및 체결 웹소켓을 모두 시작합니다. 기존 연결이 있으면 자동으로 종료 후 새로운 연결을 생성합니다."
    )
    public ResponseEntity<String> startTradingWebSocket(
            @ApiParam(value = "종목코드 (예: 005930)", required = true)
            @RequestParam String stockCode) {

        log.info("Trading page accessed - Stock: {}", stockCode);

        try {
            if (stockCode == null || stockCode.trim().isEmpty()) {
                log.warn("Invalid stock code provided: {}", stockCode);
                return ResponseEntity.badRequest().body("유효하지 않은 종목코드입니다.");
            }

            // ✅ 먼저 기존 연결들을 모두 종료 (중요!)
            log.info("Stopping existing WebSocket connections before starting new ones...");
            try {
                RealtimeBidsAndAsksClient.stopWebSocket();
            } catch (Exception e) {
                log.warn("Error stopping existing bids/asks WebSocket: {}", e.getMessage());
            }

            try {
                org.scoula.api.mocktrading.RealtimeNxtBidsAndAsksClient.stopWebSocket();
            } catch (Exception e) {
                log.warn("Error stopping existing NXT bids/asks WebSocket: {}", e.getMessage());
            }

            try {
                RealtimeExecutionClient.stopWebSocket();
                // ✅ 체결 데이터 초기화도 함께
                RealtimeExecutionClient.clearStartedStocks();
            } catch (Exception e) {
                log.warn("Error stopping existing execution WebSocket: {}", e.getMessage());
            }

            // 짧은 대기 시간 (연결 정리 완료 대기)
            Thread.sleep(500);

            // ✅ 새로운 연결 시작
            // 호가 웹소켓
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime krxClose = java.time.LocalTime.of(15, 30);
            if (now.isBefore(krxClose)) {
                RealtimeBidsAndAsksClient.startWebSocket(stockCode);
                log.info("Successfully started realtime WebSocket for stock: {} (KRX)", stockCode);
            } else {
                org.scoula.api.mocktrading.RealtimeNxtBidsAndAsksClient.startWebSocket(stockCode);
                log.info("Successfully started realtime WebSocket for stock: {} (NXT)", stockCode);
            }

            // 체결 웹소켓
            RealtimeExecutionClient.startWebSocket(stockCode);
            log.info("Successfully started execution WebSocket for stock: {}", stockCode);

            return ResponseEntity.ok("실시간 호가 및 체결 웹소켓이 시작되었습니다: " + stockCode);

        } catch (Exception e) {
            log.error("Error starting WebSocket for stock: {}", stockCode, e);
            return ResponseEntity.internalServerError().body("웹소켓 시작 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ✅ 새로 추가: WebSocket 종료 API
    @DeleteMapping("/trading")
    @ApiOperation(
            value = "실시간 웹소켓(호가+체결) 종료",
            notes = "현재 실행 중인 모든 실시간 웹소켓 연결을 종료합니다. 페이지 이동이나 애플리케이션 종료 시 호출됩니다."
    )
    public ResponseEntity<String> stopTradingWebSocket() {
        log.info("Stopping all trading WebSocket connections...");

        int successCount = 0;
        int totalCount = 3; // 호가(KRX) + 호가(NXT) + 체결

        // 호가 웹소켓 종료 (KRX)
        try {
            RealtimeBidsAndAsksClient.stopWebSocket();
            successCount++;
            log.info("Successfully stopped KRX bids/asks WebSocket");
        } catch (Exception e) {
            log.error("Error stopping KRX bids/asks WebSocket: {}", e.getMessage());
        }

        // 호가 웹소켓 종료 (NXT)
        try {
            org.scoula.api.mocktrading.RealtimeNxtBidsAndAsksClient.stopWebSocket();
            successCount++;
            log.info("Successfully stopped NXT bids/asks WebSocket");
        } catch (Exception e) {
            log.error("Error stopping NXT bids/asks WebSocket: {}", e.getMessage());
        }

        // 체결 웹소켓 종료
        try {
            RealtimeExecutionClient.stopWebSocket();
            RealtimeExecutionClient.clearStartedStocks(); // 데이터 초기화도 함께
            successCount++;
            log.info("Successfully stopped execution WebSocket");
        } catch (Exception e) {
            log.error("Error stopping execution WebSocket: {}", e.getMessage());
        }

        if (successCount == totalCount) {
            return ResponseEntity.ok("모든 실시간 웹소켓 연결이 성공적으로 종료되었습니다");
        } else {
            return ResponseEntity.ok(String.format("웹소켓 종료 완료: %d/%d 성공", successCount, totalCount));
        }
    }

    // ✅ 새로 추가: WebSocket 상태 확인 API (디버깅용)
    @GetMapping("/trading/status")
    @ApiOperation(
            value = "웹소켓 연결 상태 확인",
            notes = "현재 실시간 웹소켓들의 연결 상태를 확인합니다. 디버깅 및 모니터링 용도로 사용됩니다."
    )
    public ResponseEntity<Object> getTradingWebSocketStatus() {
        try {
            // 연결 상태 정보 수집
            java.util.Map<String, Object> status = new java.util.HashMap<>();

            // 체결 WebSocket 상태
            status.put("executionConnected", RealtimeExecutionClient.isConnected());

            // 호가 WebSocket 상태는 클라이언트에 isConnected() 메서드가 있다면 추가
            // status.put("bidsAsksConnected", RealtimeBidsAndAsksClient.isConnected());

            status.put("timestamp", java.time.LocalDateTime.now().toString());
            status.put("marketTime", java.time.LocalTime.now().isBefore(java.time.LocalTime.of(15, 30)) ? "KRX" : "NXT");

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Error checking WebSocket status: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("상태 확인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/order")
    @ApiOperation(value = "모의 주문(매수/매도)", notes = "가상의 주식 매수/매도 주문을 접수합니다")
    @ApiResponses({
            @ApiResponse(code = 200, message = "주문이 성공적으로 접수되었습니다"),
            @ApiResponse(code = 400, message = "잘못된 요청 데이터입니다"),
            @ApiResponse(code = 401, message = "로그인이 필요합니다")
    })
    public ResponseEntity<String> orderStock(
            @ApiParam(value = "주문 요청 정보", required = true)
            @RequestBody OrderRequestDto orderRequest,
            @ApiIgnore javax.servlet.http.HttpSession session
    ) {
        // 1. 세션에서 로그인 사용자 정보 조회
        org.scoula.domain.Auth.vo.UserVo loginUser = (org.scoula.domain.Auth.vo.UserVo) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        Integer userId = loginUser.getId();

        // 2. userId로 accountId 조회 (user_accounts 테이블에서)
        Integer accountId = null;
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.get("jdbc.url"),
                ConfigManager.get("jdbc.username"),
                ConfigManager.get("jdbc.password"))) {

            String findAccountSql = "SELECT account_id FROM user_accounts WHERE user_id = ?";
            try (PreparedStatement findStmt = conn.prepareStatement(findAccountSql)) {
                findStmt.setInt(1, userId);
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        accountId = rs.getInt("account_id");
                    }
                }
            }
        } catch (Exception e) {
            log.error("계좌 ID 조회 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("계좌 ID 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        if (accountId == null || accountId <= 0) {
            return ResponseEntity.badRequest().body("계좌 정보가 존재하지 않습니다.");
        }

        // 3. 입력값 검증
        if (orderRequest.getQuantity() <= 0 ||
                orderRequest.getTargetPrice() <= 0 ||
                orderRequest.getStockCode() == null ||
                (!"BUY".equalsIgnoreCase(orderRequest.getOrderType()) && !"SELL".equalsIgnoreCase(orderRequest.getOrderType()))) {
            return ResponseEntity.badRequest().body("잘못된 주문 정보입니다.");
        }

        long requiredAmount = (long) orderRequest.getQuantity() * orderRequest.getTargetPrice();

        // 4. 매수 주문일 경우 보유 현금(current_balance) 차감
        if ("BUY".equalsIgnoreCase(orderRequest.getOrderType())) {
            try (Connection conn = DriverManager.getConnection(
                    ConfigManager.get("jdbc.url"),
                    ConfigManager.get("jdbc.username"),
                    ConfigManager.get("jdbc.password"))) {

                // 4-1. 현재 잔고 조회
                String balanceSql = "SELECT current_balance FROM user_accounts WHERE account_id = ?";
                try (PreparedStatement balanceStmt = conn.prepareStatement(balanceSql)) {
                    balanceStmt.setInt(1, accountId);
                    try (ResultSet rs = balanceStmt.executeQuery()) {
                        if (rs.next()) {
                            long balance = rs.getLong("current_balance");
                            if (balance < requiredAmount) {
                                return ResponseEntity.badRequest().body("잔고가 부족합니다.");
                            }
                        } else {
                            return ResponseEntity.badRequest().body("계좌 정보를 찾을 수 없습니다.");
                        }
                    }
                }

                // 4-2. 잔고 차감
                String updateSql = "UPDATE user_accounts SET current_balance = current_balance - ? WHERE account_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, requiredAmount);
                    updateStmt.setInt(2, accountId);
                    int updateResult = updateStmt.executeUpdate();
                    if (updateResult != 1) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("잔고 차감에 실패했습니다.");
                    }
                }

                // 5. 주문 저장
                String sql = "INSERT INTO pending_orders " +
                        "(account_id, stock_code, stock_name, order_type, quantity, target_price, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, accountId);
                    pstmt.setString(2, orderRequest.getStockCode());
                    pstmt.setString(3, orderRequest.getStockName());
                    pstmt.setString(4, orderRequest.getOrderType());
                    pstmt.setInt(5, orderRequest.getQuantity());
                    pstmt.setInt(6, orderRequest.getTargetPrice());

                    int result = pstmt.executeUpdate();
                    if (result == 1) {
                        return ResponseEntity.ok(
                                String.format("주문이 성공적으로 접수되었습니다. [%s] %s %d주 @ %d원",
                                        orderRequest.getOrderType(),
                                        orderRequest.getStockCode(),
                                        orderRequest.getQuantity(),
                                        orderRequest.getTargetPrice())
                        );
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("주문 저장에 실패했습니다.");
                    }
                }
            } catch (Exception e) {
                log.error("DB 주문 저장/잔고 차감 오류: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("주문 저장 또는 잔고 차감 중 오류가 발생했습니다: " + e.getMessage());
            }
        } else {
            // 매도 주문일 경우 보유 주식 차감
            try (Connection conn = DriverManager.getConnection(
                    ConfigManager.get("jdbc.url"),
                    ConfigManager.get("jdbc.username"),
                    ConfigManager.get("jdbc.password"))) {

                // 1. 보유 주식 수량 조회
                String selectHoldingSql = "SELECT quantity FROM holdings WHERE account_id = ? AND stock_code = ?";
                int holdingQuantity = 0;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectHoldingSql)) {
                    selectStmt.setInt(1, accountId);
                    selectStmt.setString(2, orderRequest.getStockCode());
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            holdingQuantity = rs.getInt("quantity");
                        } else {
                            return ResponseEntity.badRequest().body("해당 종목의 보유 주식이 없습니다.");
                        }
                    }
                }

                // 2. 보유 주식 수량 부족 시 에러 반환
                if (holdingQuantity < orderRequest.getQuantity()) {
                    return ResponseEntity.badRequest().body("보유 주식 수량이 부족합니다.");
                }

                // 3. 보유 주식 수량 차감
                String updateHoldingSql = "UPDATE holdings SET quantity = quantity - ? WHERE account_id = ? AND stock_code = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateHoldingSql)) {
                    updateStmt.setInt(1, orderRequest.getQuantity());
                    updateStmt.setInt(2, accountId);
                    updateStmt.setString(3, orderRequest.getStockCode());
                    int updateResult = updateStmt.executeUpdate();
                    if (updateResult != 1) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("보유 주식 차감에 실패했습니다.");
                    }
                }

                // 4. 주문 저장
                String sql = "INSERT INTO pending_orders " +
                        "(account_id, stock_code, stock_name, order_type, quantity, target_price, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, accountId);
                    pstmt.setString(2, orderRequest.getStockCode());
                    pstmt.setString(3, orderRequest.getStockName());
                    pstmt.setString(4, orderRequest.getOrderType());
                    pstmt.setInt(5, orderRequest.getQuantity());
                    pstmt.setInt(6, orderRequest.getTargetPrice());

                    int result = pstmt.executeUpdate();
                    if (result == 1) {
                        return ResponseEntity.ok(
                                String.format("주문이 성공적으로 접수되었습니다. [%s] %s %d주 @ %d원",
                                        orderRequest.getOrderType(),
                                        orderRequest.getStockCode(),
                                        orderRequest.getQuantity(),
                                        orderRequest.getTargetPrice())
                        );
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("주문 저장에 실패했습니다.");
                    }
                }
            } catch (Exception e) {
                log.error("DB 주문 저장/보유 주식 차감 오류: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("주문 저장 또는 보유 주식 차감 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    @GetMapping("/orders")
    @ApiOperation(value = "주문 대기 내역 조회", notes = "세션에서 로그인 사용자 정보로 계좌 ID를 조회하여 주문 대기 내역을 반환합니다")
    @ApiResponses({
            @ApiResponse(code = 200, message = "주문 내역 조회 성공"),
            @ApiResponse(code = 400, message = "잘못된 요청 데이터입니다"),
            @ApiResponse(code = 401, message = "로그인이 필요합니다"),
            @ApiResponse(code = 500, message = "서버 내부 오류가 발생했습니다")
    })
    public ResponseEntity<?> getOrders(javax.servlet.http.HttpSession session) {
        // 1. 세션에서 로그인 사용자 정보 조회
        org.scoula.domain.Auth.vo.UserVo loginUser = (org.scoula.domain.Auth.vo.UserVo) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        Integer userId = loginUser.getId();

        // 2. userId로 accountId 조회 (user_accounts 테이블에서)
        Integer accountId = null;
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.get("jdbc.url"),
                ConfigManager.get("jdbc.username"),
                ConfigManager.get("jdbc.password"))) {

            String findAccountSql = "SELECT account_id FROM user_accounts WHERE user_id = ?";
            try (PreparedStatement findStmt = conn.prepareStatement(findAccountSql)) {
                findStmt.setInt(1, userId);
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        accountId = rs.getInt("account_id");
                    }
                }
            }
        } catch (Exception e) {
            log.error("계좌 ID 조회 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("계좌 ID 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        if (accountId == null || accountId <= 0) {
            return ResponseEntity.badRequest().body("계좌 정보가 존재하지 않습니다.");
        }

        // 3. 주문 내역 조회
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.get("jdbc.url"),
                ConfigManager.get("jdbc.username"),
                ConfigManager.get("jdbc.password"))) {

            String sql = "SELECT order_id, account_id, stock_code, stock_name, order_type, quantity, target_price, created_at " +
                    "FROM pending_orders WHERE account_id = ? ORDER BY created_at DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, accountId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    java.util.List<org.scoula.domain.mocktrading.PendingOrderDto> orders = new java.util.ArrayList<>();
                    while (rs.next()) {
                        org.scoula.domain.mocktrading.PendingOrderDto dto = new org.scoula.domain.mocktrading.PendingOrderDto();
                        dto.setOrderId(rs.getInt("order_id"));
                        dto.setAccountId(rs.getInt("account_id"));
                        dto.setStockCode(rs.getString("stock_code"));
                        dto.setStockName(rs.getString("stock_name"));
                        dto.setOrderType(rs.getString("order_type"));
                        dto.setQuantity(rs.getInt("quantity"));
                        dto.setTargetPrice(rs.getInt("target_price"));
                        dto.setCreatedAt(rs.getTimestamp("created_at"));
                        orders.add(dto);
                    }
                    return ResponseEntity.ok(orders);
                }
            }
        } catch (Exception e) {
            log.error("주문 내역 조회 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("주문 내역 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/orders")
    @ApiOperation(value = "여러 주문 취소", notes = "여러 주문 ID를 한 번에 받아 대기 주문을 일괄 취소합니다.")
    public ResponseEntity<?> cancelOrders(
            @ApiParam(value = "취소할 주문 ID 목록", required = true, example = "[101,102,103]")
            @RequestBody java.util.List<Integer> orderIds,
            @ApiIgnore javax.servlet.http.HttpSession session
    ) {
        // 1. 세션에서 로그인 사용자 정보 조회
        org.scoula.domain.Auth.vo.UserVo loginUser = (org.scoula.domain.Auth.vo.UserVo) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        Integer userId = loginUser.getId();

        if (orderIds == null || orderIds.isEmpty()) {
            return ResponseEntity.badRequest().body("주문 ID 목록이 비어 있습니다.");
        }

        try (Connection conn = DriverManager.getConnection(
                ConfigManager.get("jdbc.url"),
                ConfigManager.get("jdbc.username"),
                ConfigManager.get("jdbc.password"))) {

            // 2. 주문 정보 조회 및 본인 계좌 확인
            String selectSql = "SELECT order_id, account_id, quantity, target_price, order_type, stock_code FROM pending_orders WHERE order_id IN (" +
                    orderIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(",")) + ")";
            java.util.Map<Integer, java.util.Map<String, Object>> orderInfoMap = new java.util.HashMap<>();
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                for (int i = 0; i < orderIds.size(); i++) {
                    selectStmt.setInt(i + 1, orderIds.get(i));
                }
                try (ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        int orderId = rs.getInt("order_id");
                        java.util.Map<String, Object> info = new java.util.HashMap<>();
                        info.put("account_id", rs.getInt("account_id"));
                        info.put("quantity", rs.getInt("quantity"));
                        info.put("target_price", rs.getInt("target_price"));
                        info.put("order_type", rs.getString("order_type"));
                        info.put("stock_code", rs.getString("stock_code"));
                        orderInfoMap.put(orderId, info);
                    }
                }
            }

            if (orderInfoMap.size() != orderIds.size()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("일부 주문을 찾을 수 없습니다.");
            }

            // 3. 본인 계좌의 주문만 취소 가능
            for (java.util.Map.Entry<Integer, java.util.Map<String, Object>> entry : orderInfoMap.entrySet()) {
                int accountId = (int) entry.getValue().get("account_id");
                String checkAccountSql = "SELECT user_id FROM user_accounts WHERE account_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkAccountSql)) {
                    checkStmt.setInt(1, accountId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next() || rs.getInt("user_id") != userId) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body("본인 계좌의 주문만 취소할 수 있습니다. (orderId: " + entry.getKey() + ")");
                        }
                    }
                }
            }

            // 4. 주문 삭제
            String deleteSql = "DELETE FROM pending_orders WHERE order_id IN (" +
                    orderIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(",")) + ")";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                for (int i = 0; i < orderIds.size(); i++) {
                    deleteStmt.setInt(i + 1, orderIds.get(i));
                }
                int deleted = deleteStmt.executeUpdate();
                if (deleted != orderIds.size()) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("일부 주문 삭제에 실패했습니다.");
                }
            }

            // 5. 주문 복구 처리 (매수: 잔고, 매도: 보유주식)
            for (java.util.Map<String, Object> info : orderInfoMap.values()) {
                int accountId = (int) info.get("account_id");
                int quantity = (int) info.get("quantity");
                int targetPrice = (int) info.get("target_price");
                String orderType = (String) info.get("order_type");
                String stockCode = (String) info.get("stock_code");

                if ("BUY".equalsIgnoreCase(orderType)) {
                    long refundAmount = (long) quantity * targetPrice;
                    String updateBalanceSql = "UPDATE user_accounts SET current_balance = current_balance + ? WHERE account_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateBalanceSql)) {
                        updateStmt.setLong(1, refundAmount);
                        updateStmt.setInt(2, accountId);
                        updateStmt.executeUpdate();
                    }
                } else if ("SELL".equalsIgnoreCase(orderType)) {
                    String updateHoldingSql = "UPDATE holdings SET quantity = quantity + ? WHERE account_id = ? AND stock_code = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateHoldingSql)) {
                        updateStmt.setInt(1, quantity);
                        updateStmt.setInt(2, accountId);
                        updateStmt.setString(3, stockCode);
                        updateStmt.executeUpdate();
                    }
                }
            }

            return ResponseEntity.ok("주문이 성공적으로 일괄 취소되었습니다.");
        } catch (Exception e) {
            log.error("일괄 주문 취소/복구 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("주문 일괄 취소 또는 복구 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/order/market")
    @ApiOperation(value = "시장가 주문 체결", notes = "시장가로 즉시 체결되는 거래를 등록하고 잔고 및 보유 주식 정보를 반영합니다")
    @ApiResponses({
            @ApiResponse(code = 200, message = "시장가 주문이 성공적으로 체결되었습니다"),
            @ApiResponse(code = 400, message = "잘못된 요청 데이터입니다"),
            @ApiResponse(code = 401, message = "로그인이 필요합니다"),
            @ApiResponse(code = 500, message = "서버 내부 오류가 발생했습니다")
    })
    public ResponseEntity<String> orderMarket(
            @ApiParam(value = "시장가 주문 요청 정보", required = true)
            @RequestBody MarketOrderRequestDto marketOrderRequest,
            @ApiIgnore javax.servlet.http.HttpSession session
    ) {
        // 1. 세션에서 로그인 사용자 정보 조회
        org.scoula.domain.Auth.vo.UserVo loginUser = (org.scoula.domain.Auth.vo.UserVo) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        Integer userId = loginUser.getId();

        // 2. userId로 accountId 조회
        Integer accountId = null;
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.get("jdbc.url"),
                ConfigManager.get("jdbc.username"),
                ConfigManager.get("jdbc.password"))) {

            String findAccountSql = "SELECT account_id FROM user_accounts WHERE user_id = ?";
            try (PreparedStatement findStmt = conn.prepareStatement(findAccountSql)) {
                findStmt.setInt(1, userId);
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        accountId = rs.getInt("account_id");
                    }
                }
            }

            if (accountId == null || accountId <= 0) {
                return ResponseEntity.badRequest().body("계좌 정보가 존재하지 않습니다.");
            }

            // 3. 입력값 검증
            if (marketOrderRequest.getQuantity() <= 0 ||
                    marketOrderRequest.getMarketPrice() <= 0 ||
                    marketOrderRequest.getStockCode() == null ||
                    marketOrderRequest.getStockName() == null ||
                    marketOrderRequest.getTransactionType() == null) {
                return ResponseEntity.badRequest().body("잘못된 주문 정보입니다.");
            }

            long totalAmount = (long) marketOrderRequest.getQuantity() * marketOrderRequest.getMarketPrice();

            // 4. 매수 주문일 경우 잔고 차감
            if ("BUY".equalsIgnoreCase(marketOrderRequest.getTransactionType())) {
                // 4-1. 현재 잔고 조회
                String balanceSql = "SELECT current_balance FROM user_accounts WHERE account_id = ?";
                try (PreparedStatement balanceStmt = conn.prepareStatement(balanceSql)) {
                    balanceStmt.setInt(1, accountId);
                    try (ResultSet rs = balanceStmt.executeQuery()) {
                        if (rs.next()) {
                            long balance = rs.getLong("current_balance");
                            if (balance < totalAmount) {
                                return ResponseEntity.badRequest().body("잔고가 부족합니다.");
                            }
                        } else {
                            return ResponseEntity.badRequest().body("계좌 정보를 찾을 수 없습니다.");
                        }
                    }
                }

                // 4-2. 잔고 차감
                String updateBalanceSql = "UPDATE user_accounts SET current_balance = current_balance - ? WHERE account_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateBalanceSql)) {
                    updateStmt.setLong(1, totalAmount);
                    updateStmt.setInt(2, accountId);
                    int updateResult = updateStmt.executeUpdate();
                    if (updateResult != 1) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("잔고 차감에 실패했습니다.");
                    }
                }

                // 5. 거래 정보 DB 저장
                String sql = "INSERT INTO transactions " +
                        "(account_id, stock_code, stock_name, transaction_type, order_type, quantity, price, total_amount, executed_at, order_created_at, order_price) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, accountId);
                    pstmt.setString(2, marketOrderRequest.getStockCode());
                    pstmt.setString(3, marketOrderRequest.getStockName());
                    pstmt.setString(4, marketOrderRequest.getTransactionType());
                    pstmt.setString(5, "MARKET"); // order_type 고정
                    pstmt.setInt(6, marketOrderRequest.getQuantity());
                    pstmt.setInt(7, marketOrderRequest.getMarketPrice());
                    pstmt.setLong(8, totalAmount);
                    pstmt.setNull(9, java.sql.Types.INTEGER); // order_price는 null
                    pstmt.executeUpdate();
                }

                // 6. holdings 테이블 업데이트
                String selectHoldingSql = "SELECT holding_id, quantity, average_price, total_cost FROM holdings WHERE account_id = ? AND stock_code = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectHoldingSql)) {
                    selectStmt.setInt(1, accountId);
                    selectStmt.setString(2, marketOrderRequest.getStockCode());
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            // 기존 보유 종목: 수량, 총금액, 평균단가 업데이트
                            int holdingId = rs.getInt("holding_id");
                            int prevQuantity = rs.getInt("quantity");
                            long prevTotalCost = rs.getLong("total_cost");
                            int prevAveragePrice = rs.getInt("average_price");

                            int newQuantity = prevQuantity + marketOrderRequest.getQuantity();
                            long newTotalCost = prevTotalCost + totalAmount;
                            int newAveragePrice = (int) (newTotalCost / newQuantity);

                            String updateHoldingSql = "UPDATE holdings SET quantity = ?, average_price = ?, total_cost = ?, updated_at = CURRENT_TIMESTAMP, current_price = NULL, current_value = NULL, profit_loss = NULL, profit_rate = NULL WHERE holding_id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateHoldingSql)) {
                                updateStmt.setInt(1, newQuantity);
                                updateStmt.setInt(2, newAveragePrice);
                                updateStmt.setLong(3, newTotalCost);
                                updateStmt.setInt(4, holdingId);
                                updateStmt.executeUpdate();
                            }
                        } else {
                            // 최초 구매: 새 row 추가
                            String insertHoldingSql = "INSERT INTO holdings " +
                                    "(account_id, stock_code, stock_name, quantity, average_price, total_cost, created_at, updated_at, current_price, current_value, profit_loss, profit_rate) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertHoldingSql)) {
                                insertStmt.setInt(1, accountId);
                                insertStmt.setString(2, marketOrderRequest.getStockCode());
                                insertStmt.setString(3, marketOrderRequest.getStockName());
                                insertStmt.setInt(4, marketOrderRequest.getQuantity());
                                insertStmt.setInt(5, marketOrderRequest.getMarketPrice());
                                insertStmt.setLong(6, totalAmount);
                                insertStmt.executeUpdate();
                            }
                        }
                    }
                }

                return ResponseEntity.ok("시장가 매수 주문이 성공적으로 체결되었습니다.");
            } else if ("SELL".equalsIgnoreCase(marketOrderRequest.getTransactionType())) {
                // 1. holdings에서 보유 주식 수량 조회
                String selectHoldingSql = "SELECT holding_id, quantity, average_price, total_cost FROM holdings WHERE account_id = ? AND stock_code = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectHoldingSql)) {
                    selectStmt.setInt(1, accountId);
                    selectStmt.setString(2, marketOrderRequest.getStockCode());
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            int holdingId = rs.getInt("holding_id");
                            int prevQuantity = rs.getInt("quantity");
                            long prevTotalCost = rs.getLong("total_cost");
                            int prevAveragePrice = rs.getInt("average_price");

                            // 2. 판매 수량이 보유 수량보다 많으면 에러
                            if (prevQuantity < marketOrderRequest.getQuantity()) {
                                return ResponseEntity.badRequest().body("보유 주식 수량이 부족합니다.");
                            }

                            int newQuantity = prevQuantity - marketOrderRequest.getQuantity();
                            long sellAmount = totalAmount;
                            long newTotalCost = prevTotalCost - (long) prevAveragePrice * marketOrderRequest.getQuantity();

                            // 3. 잔고 증가
                            String updateBalanceSql = "UPDATE user_accounts SET current_balance = current_balance + ? WHERE account_id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateBalanceSql)) {
                                updateStmt.setLong(1, sellAmount);
                                updateStmt.setInt(2, accountId);
                                int updateResult = updateStmt.executeUpdate();
                                if (updateResult != 1) {
                                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                            .body("잔고 복구에 실패했습니다.");
                                }
                            }

                            // 4. 거래 정보 DB 저장
                            String sql = "INSERT INTO transactions " +
                                    "(account_id, stock_code, stock_name, transaction_type, order_type, quantity, price, total_amount, executed_at, order_created_at, order_price) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)";
                            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                                pstmt.setInt(1, accountId);
                                pstmt.setString(2, marketOrderRequest.getStockCode());
                                pstmt.setString(3, marketOrderRequest.getStockName());
                                pstmt.setString(4, marketOrderRequest.getTransactionType());
                                pstmt.setString(5, "MARKET");
                                pstmt.setInt(6, marketOrderRequest.getQuantity());
                                pstmt.setInt(7, marketOrderRequest.getMarketPrice());
                                pstmt.setLong(8, sellAmount);
                                pstmt.setNull(9, java.sql.Types.INTEGER);
                                pstmt.executeUpdate();
                            }

                            if (newQuantity > 0) {
                                // 5. holdings 업데이트 (수량/총금액/평균단가)
                                String updateHoldingSql = "UPDATE holdings SET quantity = ?, total_cost = ?, updated_at = CURRENT_TIMESTAMP, current_price = NULL, current_value = NULL, profit_loss = NULL, profit_rate = NULL WHERE holding_id = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateHoldingSql)) {
                                    updateStmt.setInt(1, newQuantity);
                                    updateStmt.setLong(2, newTotalCost);
                                    updateStmt.setInt(3, holdingId);
                                    updateStmt.executeUpdate();
                                }
                            } else {
                                // 6. holdings에서 row 삭제 (수량 0)
                                String deleteHoldingSql = "DELETE FROM holdings WHERE holding_id = ?";
                                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteHoldingSql)) {
                                    deleteStmt.setInt(1, holdingId);
                                    deleteStmt.executeUpdate();
                                }
                            }

                            return ResponseEntity.ok("시장가 매도 주문이 성공적으로 체결되었습니다.");
                        } else {
                            return ResponseEntity.badRequest().body("해당 종목의 보유 주식이 없습니다.");
                        }
                    }
                } catch (Exception e) {
                    log.error("시장가 매도 주문 처리 오류: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("시장가 매도 주문 처리 중 오류가 발생했습니다: " + e.getMessage());
                }
            } else {
                return ResponseEntity.badRequest().body("알 수 없는 거래 유형입니다.");
            }
        } catch (Exception e) {
            log.error("시장가 주문 저장/잔고/보유주식 반영 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("시장가 주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}