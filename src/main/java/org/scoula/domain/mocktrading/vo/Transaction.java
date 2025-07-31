package org.scoula.domain.mocktrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    private Integer transactionId;      // 거래내역 ID
    private Integer accountId;          // 계좌 ID
    private String stockCode;           // 종목코드
    private String stockName;           // 종목명
    private String transactionType;     // 거래 타입 (BUY, SELL)
    private String orderType;           // 주문 유형 (MARKET, LIMIT)
    private Integer quantity;           // 주문 수량
    private Integer price;              // 체결 가격
    private Integer orderPrice;         // 주문 가격 (지정가 주문 시)
    private Long totalAmount;           // 총 거래금액
    private Timestamp executedAt;       // 실행 날짜
    private Timestamp orderCreatedAt;   // 주문 생성 날짜

    // 추가 필드 (조회 시 사용)
    private String status;              // 거래 상태 (COMPLETED, CANCELLED, PENDING)
    private String imageUrl;            // 종목 로고 이미지 URL
}