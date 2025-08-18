package org.scoula.service.chatbot.handler.misc;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.scoula.util.chatbot.OpenAiClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// Message
public class DefaultMessageHandler implements IntentHandler {

    private final OpenAiClient openAiClient;

    @Override public IntentType supports() { return IntentType.MESSAGE; }

    @Override
    public ExecutionResult handle(ExecutionContext ctx) {
        String content = openAiClient.getChatCompletion(ctx.getUserMessage());
        return ExecutionResult.builder().finalContent(content).build();
    }
}
