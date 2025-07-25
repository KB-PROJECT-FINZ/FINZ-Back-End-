package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "챗봇 질문 요청 정보")
public class ChatRequestDto {
    
    @ApiModelProperty(value = "사용자 질문 메시지", required = true, example = "삼성전자 주식에 대해 알려주세요", notes = "챗봇에게 전달할 사용자의 질문")
    private String message;
}
