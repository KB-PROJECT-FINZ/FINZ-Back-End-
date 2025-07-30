package org.scoula.controller.trading;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.service.trading.TradingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trading")
public class TradingController {

    private final TradingService tradingService;

    // 사용자 모의투자 거래 내역 조회
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<List<TransactionDTO>> getUserTransactions(@PathVariable int userId) {
        try {
            List<TransactionDTO> transactions = tradingService.getUserTransactions(userId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}