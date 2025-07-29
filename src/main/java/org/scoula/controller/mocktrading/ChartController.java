package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.api.mocktrading.MinuteChartApi;
import org.scoula.api.mocktrading.RealtimeBidsAndAsksClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chart")
@RequiredArgsConstructor
@Api(tags = "Chart API", description = "주식 차트 데이터 관련 API")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
public class ChartController {

    private final MinuteChartApi minuteChartApi;

    @GetMapping("/minute/{stockCode}")
    @ApiOperation(
            value = "분봉 차트 데이터 조회",
            notes = "한국투자증권 API에서 제공하는 원본 분봉 차트 데이터를 그대로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 204, message = "데이터 없음"),
            @ApiResponse(code = 400, message = "잘못된 요청"),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    public ResponseEntity<JsonNode> getMinuteChart(
            @ApiParam(value = "종목코드 (예: 005930)", required = true)
            @PathVariable String stockCode) {

        log.info("Received request for minute chart - Stock: {}", stockCode);

        try {
            // 입력값 검증
            if (stockCode == null || stockCode.trim().isEmpty()) {
                log.warn("Invalid stock code provided: {}", stockCode);
                return ResponseEntity.badRequest().build();
            }

            // 원본 차트 데이터 조회
            JsonNode chartData = minuteChartApi.getRawMinuteChartData(stockCode);

            if (chartData == null) {
                log.info("No chart data found for stock: {}", stockCode);
                return ResponseEntity.noContent().build();
            }

            log.info("Successfully retrieved chart data for stock: {}", stockCode);
            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            log.error("Error processing minute chart request for stock: {}", stockCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/minute/{stockCode}/fullday")
    @ApiOperation(
            value = "일일 전체 분봉 차트 데이터 조회",
            notes = "09:00부터 현재까지의 모든 분봉 차트 데이터를 병합하여 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 204, message = "데이터 없음"),
            @ApiResponse(code = 400, message = "잘못된 요청"),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    public ResponseEntity<JsonNode> getFullDayMinuteChart(
            @ApiParam(value = "종목코드 (예: 005930)", required = true)
            @PathVariable String stockCode) {

        log.info("Received request for full day minute chart - Stock: {}", stockCode);

        try {
            // 입력값 검증
            if (stockCode == null || stockCode.trim().isEmpty()) {
                log.warn("Invalid stock code provided: {}", stockCode);
                return ResponseEntity.badRequest().build();
            }

            // 일일 전체 차트 데이터 조회
            JsonNode chartData = minuteChartApi.getFullDayMinuteChartData(stockCode);

            if (chartData == null) {
                log.info("No full day chart data found for stock: {}", stockCode);
                return ResponseEntity.noContent().build();
            }

            log.info("Successfully retrieved full day chart data for stock: {}", stockCode);
            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            log.error("Error processing full day minute chart request for stock: {}", stockCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/trading")
    @RequestMapping({
            "/trading",
            "/mock-trading/**",
            "/chart/**"
            // 필요하면 다른 SPA 라우트도 추가
    })
    @ApiOperation(
            value = "트레이딩 페이지 접근 시 실시간 웹소켓 시작",
            notes = "stockCode 파라미터를 받아 해당 종목의 실시간 호가 웹소켓을 시작합니다."
    )
    public ResponseEntity<String> startTradingWebSocket(
            @ApiParam(value = "종목코드 (예: 005930)", required = true)
            @RequestParam String stockCode,
            @RequestParam(required = false) String stockName,
            @RequestParam(required = false) String tab) {

        log.info("Trading page accessed - Stock: {} ({}), Tab: {}", stockCode, stockName, tab);

        try {
            // 입력값 검증
            if (stockCode == null || stockCode.trim().isEmpty()) {
                log.warn("Invalid stock code provided: {}", stockCode);
                return ResponseEntity.badRequest().body("유효하지 않은 종목코드입니다.");
            }


            // 15:30 이후에는 NXT, 이전에는 기존 KRX
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime krxClose = java.time.LocalTime.of(15, 30);
            if (now.isBefore(krxClose)) {
                RealtimeBidsAndAsksClient.startWebSocket(stockCode);
                log.info("Successfully started realtime WebSocket for stock: {} (KRX)", stockCode);
                return ResponseEntity.ok("실시간 웹소켓이 시작되었습니다: " + stockCode + " (KRX)");
            } else {
                org.scoula.api.mocktrading.RealtimeNxtBidsAndAsksClient.startWebSocket(stockCode);
                log.info("Successfully started realtime WebSocket for stock: {} (NXT)", stockCode);
                return ResponseEntity.ok("실시간 웹소켓이 시작되었습니다: " + stockCode + " (NXT)");
            }

        } catch (Exception e) {
            log.error("Error starting WebSocket for stock: {}", stockCode, e);
            return ResponseEntity.internalServerError().body("웹소켓 시작 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
