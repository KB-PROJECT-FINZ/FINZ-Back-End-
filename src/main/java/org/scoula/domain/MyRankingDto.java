package org.scoula.domain;

import io.swagger.models.auth.In;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MyRankingDto {
    private Long userId;
    private BigDecimal gainRate;
    private Integer ranking;
    private Double topPercent;

}
