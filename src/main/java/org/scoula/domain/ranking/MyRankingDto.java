package org.scoula.domain.ranking;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MyRankingDto {
    private Long userId;          // 사용자 ID
    private BigDecimal gainRate;  // 수익률
    private Integer ranking;      // 전체 순위
    private Double topPercent;    // 상위 퍼센트 (%)

}
