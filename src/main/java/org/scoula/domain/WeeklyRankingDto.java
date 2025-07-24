package org.scoula.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WeeklyRankingDto {
    private Long userId;
    private String name;
    private BigDecimal gainRate;
    private Integer ranking;
}
