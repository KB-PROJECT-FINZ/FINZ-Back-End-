package org.scoula.domain.chatbot.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.chatbot.enums.ErrorType;


import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel("쳇봇 에러 저장 dto")
public class ChatErrorDto {
    private Integer id;
    private Integer userId;
    private String errorMessage;

    private ErrorType errorType;
    private LocalDateTime createdAt;
}
