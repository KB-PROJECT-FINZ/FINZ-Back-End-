package org.scoula.domain.chatbot.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@AllArgsConstructor
@Builder
public class RecommendationStock {
    private String code;
    private String name;
    private Double price;
    private Double per;
    private Double eps;
    private Double roe;
    private Double pbr;
    private Double open;
    private Double high;
    private Double low;
    private Double volume;
    private Double avgPrice;
    private Double foreignRate;
    private Double turnRate;
    private Double high52w;
    private Double low52w;


    // GPT나 내부 계산에서 나오는 예상 수익률, 리스크 정도
    private String expectedReturn;  
    private String riskLevel;       

}
