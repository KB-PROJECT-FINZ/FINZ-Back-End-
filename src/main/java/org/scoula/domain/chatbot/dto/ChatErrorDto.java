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
@ApiModel("쳇봇 에러 저장 dto")
public class ChatErrorDto {
    private Long id;
    private Long userId;
    private String errorMessage;
    private String errorType;     // GPT / DB / API 등
    private LocalDateTime createdAt;
}
