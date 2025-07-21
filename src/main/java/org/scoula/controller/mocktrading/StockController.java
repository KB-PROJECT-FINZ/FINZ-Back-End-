package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.*;
import org.scoula.api.mocktrading.PriceApi;
import org.scoula.domain.mocktrading.BuyRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/mock")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
@Api(tags = "주식 모의투자 API", description = "주식 가격 조회 및 모의 매매 기능을 제공합니다")
public class StockController {

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