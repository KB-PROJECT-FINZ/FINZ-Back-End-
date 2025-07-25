package org.scoula.domain.ranking;

import lombok.Data;

@Data
public class TraitStockDto {
    private String name;         // 종목명
    private double gain;         // 수익률
    private String logo;         // 로고 이미지 경로
    private int attackRatio;     // 공격형 보유 비중
    private int neutralRatio;    // 중립형 보유 비중
    private int stableRatio;     // 안정형 보유 비중
}
