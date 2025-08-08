package org.scoula.domain.ranking;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MyDistributionDto {
    private String stockCode;
    private String stockName;
    private Double gainRate;
    private Integer positionIndex;
    private String positionLabel;
    private List<Integer> distributionBins;  // 예: [0, 2, 5, 10, 4, 1]
    private String color;

}