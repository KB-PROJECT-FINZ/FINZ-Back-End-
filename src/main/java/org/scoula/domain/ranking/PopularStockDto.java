package org.scoula.domain.ranking;


import lombok.Data;

@Data
public class PopularStockDto {
    private String name;    // 종목명
    private String trait;   // 성향
    private double gain;    // 수익률
    private String logo;    // 로고 이미지 경로
}
