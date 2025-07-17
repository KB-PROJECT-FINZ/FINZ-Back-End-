package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description ="GPT 요청 DTO")
public class ChatRequestDto {
    private String message;
}
