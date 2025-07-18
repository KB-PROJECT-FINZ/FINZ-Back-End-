package org.scoula.domain.chatbot.dto;


import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder 
@ApiModel(discriminator = "메시지 저장/조회 dto")
public class ChatMessageDto {
    private Long id; // 메세지 ID
    private Long userId; //유저 ID
    private Long sessionId; // 세션 ID
    private String role;  // "user" / "assistant"
    private String content;  // 메세지 내용
    private String intentType;  // optional
    private LocalDateTime createdAt;  //생성 일시
}
