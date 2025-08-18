package org.scoula.service.chatbot.handler;

import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;

public interface IntentHandler {
    IntentType supports();
    ExecutionResult handle(ExecutionContext ctx) throws Exception;
}