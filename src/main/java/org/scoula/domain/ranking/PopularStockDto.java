package org.scoula.domain.ranking;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
public class PopularStockDto {
    private String stockCode;        // 종목 코드
    private String stockName;        // 종목명
    private String imageUrl;         //회사이미지
    private Integer transactionCount;// 총 거래 수 (매수+매도)
    private Integer ranking;         // 순위
}