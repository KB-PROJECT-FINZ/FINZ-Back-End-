package org.scoula.domain.mocktrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding {
    private Integer holdingId;          // 보유 종목 ID
    private Integer accountId;          // 계좌 ID
    private String stockCode;           // 종목코드
    private String stockName;           // 종목명
    private Integer quantity;           // 보유 수량
    private BigDecimal averagePrice;    // 평균 매수가
    private Long totalCost;             // 총 매수금액
    private Integer currentPrice;       // 현재가
    private Long currentValue;          // 현재 평가금액
    private Long profitLoss;            // 평가 손익
    private BigDecimal profitRate;      // 수익률
    private Timestamp createdAt;        // 생성 날짜
    private Timestamp updatedAt;        // 수정 날짜

    // 추가 필드 (조회 시 사용)
    private String imageUrl;            // 종목 로고 이미지 URL
    private Integer percentage;         // 포트폴리오 내 비중
}