package org.scoula.domain.chatbot.dto;


import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(discriminator = "종목 분석 정보 저장 dto")
public class ChatAnalysisDto {
    private Integer id;
    private String ticker;
    private String name;
    private String region;
    private Float per;
    private Float roe;
    private Float eps;
    private Float price;
    private LocalDateTime updatedAt;
    private Float pbr;
    private Float open; // 시가
    private Float high; // 고가
    private Float low;  // 저가
    private Long volume; // 누적 거래량
    private Float avgPrice; // 가중 평균 주가
    private Float foreignRate; // 외국인 보유율
    private Float turnRate;    // 거래 회전율
    private Float high52w;     // 52주 고가
    private Float low52w;      // 52주 저가
}
