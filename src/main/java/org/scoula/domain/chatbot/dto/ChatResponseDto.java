package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(discriminator = "GPT 응답 DTO")
public class ChatResponseDto {
    private String content; //gpt 응답
    private IntentType intentType; // 응답 intent (백엔드 판단 결과일 수도 있음 -> 추후 확장?)
    private Integer messageId; // 저장된 chat_messages.id (있으면)

}
