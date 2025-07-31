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
    private String price;
    private String per;
    private String eps;
    private String pbr;
    private String open;
    private String high;
    private String low;
    private String volume;
    private String avgPrice;
    private String foreignRate;
    private String turnRate;
    private String high52w;
    private String low52w;


    // GPT나 내부 계산에서 나오는 예상 수익률, 리스크 정도
    private String expectedReturn;  
    private String riskLevel;       

}
