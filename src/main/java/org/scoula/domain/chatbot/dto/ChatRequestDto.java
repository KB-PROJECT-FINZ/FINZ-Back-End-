package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.scoula.domain.chatbot.enums.IntentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "챗봇 질문 요청 정보")
public class ChatRequestDto {
    
    @ApiModelProperty(value = "사용자 질문 메시지", required = true, example = "삼성전자 주식에 대해 알려주세요", notes = "챗봇에게 전달할 사용자의 질문")
    private String message;
    //private Integer userId;  // 사용자 고유 ID
    //private Integer sessionId;   // 대화 세션 ID
    //private String message; // 사용자가 보낸 메시지
    //private IntentType intentType;  // 의도 타입: RECOMMEND_PROFILE, STOCK_ANALYZE 등

}
