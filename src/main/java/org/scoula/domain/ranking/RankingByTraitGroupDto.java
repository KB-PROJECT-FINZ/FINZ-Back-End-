package org.scoula.domain.ranking;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RankingByTraitGroupDto {
    private Long userId;             // 사용자 ID
    private String traitGroup;       // 성향 그룹
    private String originalTrait;    //원래 성향
    private BigDecimal gainRate;     // 수익률
    private Integer ranking;         // 순위
    private String nickname;         //닉네임

}
