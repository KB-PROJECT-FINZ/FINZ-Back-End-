package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import org.scoula.api.mocktrading.PriceApi;
import org.scoula.domain.mocktrading.BuyRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/mock")
public class StockController {

    @GetMapping("/price/{code}")
    public ResponseEntity<JsonNode> getStockPrice(@PathVariable("code") String code) {
        try {
            JsonNode price = PriceApi.getPriceData(code);
            return ResponseEntity.ok(price);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/buy")
    public ResponseEntity<String> buyStock(@RequestBody BuyRequestDto buyRequest) {
        // TODO: 가상 주문 처리 로직 연결 예정
        return ResponseEntity.ok("가상 매수 요청이 성공적으로 접수되었습니다.");
    }
}