package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.scoula.domain.chatbot.enums.IntentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description ="GPT 요청 DTO")
public class ChatRequestDto {

    private Integer userId;  // 사용자 고유 ID
    private Integer sessionId;   // 대화 세션 ID
    private String message; // 사용자가 보낸 메시지
    private IntentType intentType;  // 의도 타입: RECOMMEND_PROFILE, STOCK_ANALYZE 등


}
