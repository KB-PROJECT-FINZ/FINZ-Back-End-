package org.scoula.domain.ranking;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RankingByTraitGroupDto {
    private Long userId;             // 사용자 ID
    private String traitGroup;       // 성향 그룹
    private BigDecimal gainRate;     // 수익률
    private Integer ranking;         // 순위
}
