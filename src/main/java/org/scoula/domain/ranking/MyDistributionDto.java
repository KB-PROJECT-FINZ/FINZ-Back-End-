package org.scoula.domain.ranking;


import lombok.Data;


import java.util.List;

@Data
public class MyDistributionDto {
    private String stockCode;
    private String stockName;
    private Double gainRate;
    private int positionIndex;
    private String positionLabel;
    private Integer bin0;
    private Integer bin1;
    private Integer bin2;
    private Integer bin3;
    private Integer bin4;
    private Integer bin5;
    private String color;


    private List<Integer> distributionBins;
}
