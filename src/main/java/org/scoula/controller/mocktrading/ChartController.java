package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.api.mocktrading.MinuteChartApiKiwoom;
import org.scoula.api.mocktrading.VariousChartApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chart")
@RequiredArgsConstructor
@Api(tags = "Chart API", description = "주식 차트 데이터 관련 API")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
public class ChartController {

    private final MinuteChartApiKiwoom minuteChartApiKiwoom;
    private final VariousChartApi variousChartApi;

    @GetMapping("/minute/{stockCode}")
    @ApiOperation(
            value = "키움 분봉 차트 데이터 조회",
            notes = "키움증권 API에서 제공하는 원본 분봉 차트 데이터를 그대로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 204, message = "데이터 없음"),
            @ApiResponse(code = 400, message = "잘못된 요청"),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    public ResponseEntity<JsonNode> getMinuteChartKiwoom(
            @ApiParam(value = "종목코드 (예: 005930)", required = true)
            @PathVariable String stockCode
    ) {
        log.info("Received Kiwoom minute chart request - Stock: {}", stockCode);

        try {
            // 입력값 검증
            if (stockCode == null || stockCode.trim().isEmpty()) {
                log.warn("Invalid stock code provided: {}", stockCode);
                return ResponseEntity.badRequest().build();
            }

            JsonNode result = minuteChartApiKiwoom.getKiwoomMinuteChartData(stockCode);

            if (result == null) {
                log.info("No chart data found for stock: {}", stockCode);
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error processing Kiwoom minute chart request for stock: {}", stockCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/minute/batch")
    @ApiOperation(
            value = "여러 종목의 키움 분봉 차트 데이터 동시 조회",
            notes = "종목코드 배열을 받아 각 종목의 키움 분봉 차트 데이터를 동시에 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 400, message = "잘못된 요청"),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    public ResponseEntity<List<JsonNode>> getKiwoomMinuteChartBatch(
            @ApiParam(value = "종목코드 배열", required = true)
            @RequestBody List<String> stockCodes
    ) {
        log.info("Received Kiwoom minute chart batch request - Stocks: {}", stockCodes);

        if (stockCodes == null || stockCodes.isEmpty()) {
            log.warn("No stock codes provided");
            return ResponseEntity.badRequest().build();
        }

        // 동시성 처리를 위한 ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(stockCodes.size(), 10));
        List<CompletableFuture<JsonNode>> futures = new ArrayList<>();

        for (String stockCode : stockCodes) {
            CompletableFuture<JsonNode> future = CompletableFuture.supplyAsync(() -> {
                try {
                    JsonNode result = minuteChartApiKiwoom.getKiwoomMinuteChartData(stockCode, true);
                    System.out.println(result);
                    return result;
                } catch (Exception e) {
                    log.error("Error processing Kiwoom minute chart for stock: {}", stockCode, e);
                    return null;
                }
            }, executor);
            futures.add(future);
        }

        // 모든 작업 완료까지 대기
        List<JsonNode> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        executor.shutdown();

        return ResponseEntity.ok(results);
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
