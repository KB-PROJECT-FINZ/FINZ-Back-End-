package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(discriminator = "AI 투자 행동 피드백 저장 dto")
public class ChatBehaviorFeedbackDto {
    private Integer id;
    private Integer userId;
    private Integer sessionId;
    private Integer messageId;
    private String summaryText;
    private String riskText;
    private String suggestionText;

    private Integer transactionCount;
    private Integer analysisPeriod;
    private String startDate;
    private String endDate;

    private List<Long> transactionIds;
}
