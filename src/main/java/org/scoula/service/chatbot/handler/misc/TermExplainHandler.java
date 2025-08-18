package org.scoula.service.chatbot.handler.misc;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.PromptBuilder;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.scoula.util.chatbot.OpenAiClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// TERM_EXPLAIN
public class TermExplainHandler implements IntentHandler {
    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;

    @Override
    public IntentType supports() {
        return IntentType.TERM_EXPLAIN;
    }

    @Override
    public ExecutionResult handle(ExecutionContext ctx) {
        String prompt = promptBuilder.buildForTermExplain(ctx.getUserMessage());
        String content = openAiClient.getChatCompletion(prompt);
        return ExecutionResult.builder().finalContent(content).build();
    }
}
