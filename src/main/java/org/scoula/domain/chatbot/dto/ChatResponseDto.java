package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "챗봇 응답 정보")
public class ChatResponseDto {
    
    @ApiModelProperty(value = "챗봇 응답 메시지", example = "삼성전자는 대한민국의 대표적인 IT 기업으로...", notes = "AI 챗봇이 생성한 응답 내용")
    private String content;
}
