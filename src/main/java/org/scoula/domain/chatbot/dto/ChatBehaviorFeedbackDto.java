package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("strategySummary")
    private String summaryText;

    @JsonProperty("riskPoint")
    private String riskText;

    @JsonProperty("suggestion")
    private String suggestionText;

    private Integer transactionCount;

    @JsonProperty("periodDays")
    private Integer analysisPeriod;

    @JsonProperty("startDate")
    private LocalDate analysisStart;

    @JsonProperty("endDate")
    private LocalDate analysisEnd;

    private List<Long> transactionIds;
}
