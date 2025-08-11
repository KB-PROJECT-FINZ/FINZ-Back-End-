package org.scoula.domain.ranking;

import lombok.Data;

@Data
public class StockDistributionSummaryDto {
    private String stockCode;
    private String stockName;
    private Integer bin0;
    private Integer bin1;
    private Integer bin2;
    private Integer bin3;
    private Integer bin4;
    private Integer bin5;
    private String color; // 기본 색상 또는 null 가능
}
