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
    private Long id;
    private String ticker;
    private String name;
    private String region;
    private Float per;
    private Float roe;
    private Float eps;
    private Float price;
    private LocalDateTime updatedAt;
}
