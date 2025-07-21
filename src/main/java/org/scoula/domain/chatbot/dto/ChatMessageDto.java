package org.scoula.domain.chatbot.dto;


import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder 
@ApiModel(discriminator = "메시지 저장/조회 dto")
public class ChatMessageDto {
    private Integer id; // 메세지 ID
    private Integer userId; //유저 ID
    private Integer sessionId; // 세션 ID
    private String role;  // "user" / "assistant"
    private String content;  // 메세지 내용
    private IntentType intentType;  // optional
    private LocalDateTime createdAt;  //생성 일시
}
