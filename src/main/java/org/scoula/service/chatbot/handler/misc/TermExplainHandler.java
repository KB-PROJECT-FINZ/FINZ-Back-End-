package org.scoula.service.chatbot.handler.misc;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.PromptBuilder;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.scoula.util.chatbot.OpenAiClient;
import org.springframework.stereotype.Component;

/**
 * TermExplainHandler
 *
 * <p>IntentType.TERM_EXPLAIN 에 해당하는 금융 용어 설명 핸들러.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>사용자가 금융 용어나 개념에 대해 질문했을 때 동작한다.</li>
 *   <li>PromptBuilder를 통해 "용어 설명 전용 프롬프트"를 생성한다.</li>
 *   <li>OpenAiClient(GPT)에 프롬프트를 전달하여 자연어 설명 응답을 생성한다.</li>
 * </ul>
 *
 */
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
