package org.scoula.domain.ranking;


import lombok.Data;

@Data
public class PopularStockDto {
    private String stockCode;
    private String stockName;
    private Integer transactionCount;
    private Integer ranking;
}
