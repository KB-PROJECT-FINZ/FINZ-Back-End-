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
@ApiModel(discriminator = "사용자 종목추천 피드백 저장 dto")
public class ChatFeedbackDto {
    private Long id;
    private Long userId;
    private Long recommendationId;
    private String feedbackType;  // "INTERESTED", "NOT_INTERESTED"
    private LocalDateTime createdAt;
}
