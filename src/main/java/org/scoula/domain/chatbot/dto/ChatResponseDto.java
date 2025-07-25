package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "챗봇 응답 정보")
public class ChatResponseDto {
    
    @ApiModelProperty(value = "챗봇 응답 메시지", example = "삼성전자는 대한민국의 대표적인 IT 기업으로...", notes = "AI 챗봇이 생성한 응답 내용")
    private String content;

    //private String content; //gpt 응답
    //private IntentType intentType; // 응답 intent (백엔드 판단 결과일 수도 있음 -> 추후 확장?)
    //private Integer messageId; // 저장된 chat_messages.id (있으면)
    //private Integer sessionId;
}
