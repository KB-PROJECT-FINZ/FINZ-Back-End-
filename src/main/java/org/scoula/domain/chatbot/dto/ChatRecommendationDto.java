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
@ApiModel(discriminator = "종목 추천 결과 dto")
public class ChatRecommendationDto {
    private Long id;
    private Long userId;
    private String ticker;
    private String recommendType; // "PROFILE" or "KEYWORD"
    private String reason;
    private String expectedReturn;
    private String riskLevel;
    private LocalDateTime createdAt;
}
