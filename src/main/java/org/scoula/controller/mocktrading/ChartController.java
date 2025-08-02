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
import org.scoula.api.mocktrading.VariousChartApi;
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
    private final VariousChartApi variousChartApi;


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

    @GetMapping("/various/{stockCode}")
    @ApiOperation(
            value = "일/주/월/년별 차트 데이터 조회",
            notes = "한국투자증권 API에서 제공하는 일/주/월/년별 차트 데이터를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 204, message = "데이터 없음"),
            @ApiResponse(code = 400, message = "잘못된 요청"),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    public ResponseEntity<JsonNode> getVariousChart(
            @ApiParam(value = "종목코드 (예: 005930)", required = true)
            @PathVariable String stockCode,
            @ApiParam(value = "기간코드 (D:일, W:주, M:월, Y:년)", required = true)
            @RequestParam String periodCode,
            @ApiParam(value = "조회 시작일 (yyyyMMdd)", required = true)
            @RequestParam String startDate,
            @ApiParam(value = "조회 종료일 (yyyyMMdd)", required = true)
            @RequestParam String endDate) {

        log.info("Received request for various chart - Stock: {}, Period: {}, Start: {}, End: {}", stockCode, periodCode, startDate, endDate);

        try {
            if (stockCode == null || stockCode.trim().isEmpty() ||
                periodCode == null || periodCode.trim().isEmpty() ||
                startDate == null || startDate.trim().isEmpty() ||
                endDate == null || endDate.trim().isEmpty()) {
                log.warn("Invalid parameter(s) provided");
                return ResponseEntity.badRequest().build();
            }

            JsonNode chartData = variousChartApi.getChartData(stockCode, periodCode, startDate, endDate);

            if (chartData == null) {
                log.info("No various chart data found for stock: {}", stockCode);
                return ResponseEntity.noContent().build();
            }

            log.info("Successfully retrieved various chart data for stock: {}", stockCode);
            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            log.error("Error processing various chart request for stock: {}", stockCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
