package org.scoula.service.chatbot.handler.misc;

import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.springframework.stereotype.Component;

@Component
// Unknown
public class UnknownHandler implements IntentHandler {
    @Override public IntentType supports() { return IntentType.UNKNOWN; }
    @Override public ExecutionResult handle(ExecutionContext ctx) {
        return ExecutionResult.builder()
                .finalContent("요청 내용을 이해하지 못했습니다. 다시 질문해주세요.")
                .build();
    }
}