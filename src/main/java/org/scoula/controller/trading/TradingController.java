package org.scoula.controller.trading;

import lombok.RequiredArgsConstructor;
import org.scoula.config.auth.LoginUser;
import org.scoula.domain.Auth.vo.UserVo;
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
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDTO>> getUserTransactions(@LoginUser UserVo user){
        try {
            List<TransactionDTO> transactions = tradingService.getUserTransactions(user.getId());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            e.printStackTrace();
            // 에러가 발생해도 데이터가 있으면 반환
            try {
                List<TransactionDTO> transactions = tradingService.getUserTransactions(user.getId());
                return ResponseEntity.status(500).body(transactions);
            } catch (Exception ex) {
                return ResponseEntity.status(500).body(null);
            }
        }
    }


} 
