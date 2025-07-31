package org.scoula.domain.trading.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionVO {
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