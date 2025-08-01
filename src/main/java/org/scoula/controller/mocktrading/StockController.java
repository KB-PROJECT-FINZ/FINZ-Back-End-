package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.api.mocktrading.PriceApi;
import org.scoula.api.mocktrading.RealtimeBidsAndAsksClient;
import org.scoula.api.mocktrading.RealtimeExecutionClient;
import org.scoula.domain.mocktrading.BuyRequestDto;
import org.scoula.service.mocktrading.StockIndustryUpdaterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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

    @GetMapping("/price/{code}")
    @ApiOperation(value = "주식 가격 조회", notes = "주식 종목코드를 이용하여 실시간 가격 정보를 조회합니다")
    @ApiParam(name = "code", value = "주식 종목코드 (예: 005930)", required = true, example = "005930")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공적으로 가격 정보를 조회했습니다"),
            @ApiResponse(code = 500, message = "서버 내부 오류가 발생했습니다")
    })
    public ResponseEntity<JsonNode> getStockPrice(@PathVariable("code") String code) {
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

    @PostMapping("/buy")
    @ApiOperation(value = "모의 매수 주문", notes = "가상의 주식 매수 주문을 접수합니다")
    @ApiResponses({
            @ApiResponse(code = 200, message = "매수 주문이 성공적으로 접수되었습니다"),
            @ApiResponse(code = 400, message = "잘못된 요청 데이터입니다")
    })
    public ResponseEntity<String> buyStock(
            @ApiParam(value = "매수 요청 정보", required = true)
            @RequestBody BuyRequestDto buyRequest) {
        // TODO: 가상 주문 처리 로직 연결 예정
        return ResponseEntity.ok("가상 매수 요청이 성공적으로 접수되었습니다.");
    }
}