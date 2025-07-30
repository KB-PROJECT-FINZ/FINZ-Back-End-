package org.scoula.domain.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private int transactionId;
    private int accountId;
    private String stockCode;
    private String stockName;
    private String transactionType;
    private String orderType;
    private int quantity;
    private int price;
    private int orderPrice;
    private long totalAmount;
    private LocalDateTime executedAt;
    private LocalDateTime orderCreatedAt;
}
