package org.scoula.domain.ranking;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MyDistributionDto {
    private String stockName;    // 종목명
    private double gainRate;     // 내 수익률
    private int positionIndex;   // 내 위치 인덱스
    private String positionLabel;// 퍼센트 라벨
    private List<Integer> distributionBins; // 수익률 분포 구간 배열

}