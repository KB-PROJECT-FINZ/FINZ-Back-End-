package org.scoula.service.chatbot.handler.context;

import lombok.Builder;
import lombok.Getter;
import org.scoula.domain.chatbot.enums.IntentType;

@Getter
@Builder
public class ExecutionContext {
    private final Integer userId;
    private final Integer sessionId;
    private final String userMessage;
    private final IntentType intentType;
}