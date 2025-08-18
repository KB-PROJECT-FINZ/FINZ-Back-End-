package org.scoula.service.chatbot.handler.misc;

import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.springframework.stereotype.Component;

@Component
// SessionEnd
public class SessionEndHandler implements IntentHandler {
    @Override public IntentType supports() { return IntentType.SESSION_END; }
    @Override public ExecutionResult handle(ExecutionContext ctx) {
        return ExecutionResult.builder().finalContent("대화를 종료합니다. 감사합니다.").build();
    }
}