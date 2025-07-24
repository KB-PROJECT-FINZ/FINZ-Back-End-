package org.scoula.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Top5StockDto {
    private String stockCode;
    private String stockName;
    private Integer investorCount;
    private BigDecimal avgGainRate;
    private Integer ranking;
}
