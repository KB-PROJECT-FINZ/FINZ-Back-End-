package org.scoula.domain.chatbot.vo;

import java.time.LocalDateTime;

public class ChatMessage {
    
    
    // DB테이블 ->직접 매핑
    private Integer id;
    private Integer userId;
    private Integer sessionId;
    private String role;         // "user" / "assistant"
    private String content;
    private String intentType;
    private LocalDateTime createdAt;
}