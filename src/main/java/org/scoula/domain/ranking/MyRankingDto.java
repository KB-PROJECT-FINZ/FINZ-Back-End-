package org.scoula.domain.ranking;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MyRankingDto {
    private Long userId;
    private BigDecimal gainRate;
    private Integer ranking;
    private Double topPercent;

}
