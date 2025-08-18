package org.scoula.domain.ranking;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MyRankingDto {
    private Long userId;          // 사용자 ID
    private Double gainRate;      // 수익률
    private Integer ranking;      // 전체 순위
    private Double topPercent;    // 상위 퍼센트 (%)
    private String riskType;     //성향
    private String originalTrait;
    private String baseDate;   // 주간 랭킹 기준 날짜 추가


}
