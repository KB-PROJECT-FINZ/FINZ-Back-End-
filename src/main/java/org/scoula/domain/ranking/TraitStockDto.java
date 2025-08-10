package org.scoula.domain.ranking;

import lombok.Data;

@Data
public class TraitStockDto {
    private String name;         // 종목명
    private double gain;         // 수익률
    private String logo;         // 로고 이미지 경로
    private int conservativeRatio;  // 보수형 보유 비중
    private int balancedRatio;      // 균형형 보유 비중
    private int aggressiveRatio;    // 공격형 보유 비중
    private int analyticalRatio;   // 특수형(ANALYTICAL)
    private int emotionalRatio;    // 기타(EMOTIONAL)
}
