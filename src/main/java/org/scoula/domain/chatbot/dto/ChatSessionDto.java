package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(discriminator = "쳇봇 세션 DTO")
public class ChatSessionDto {
    private Integer id;                     // 세션 ID
    private Integer userId;                 // 사용자 ID (nullable)
    private LocalDateTime startedAt;        // 시작 시각
    private LocalDateTime endedAt;          // 종료 시각 (nullable)
    private IntentType lastIntent;              // 마지막 intent
}
