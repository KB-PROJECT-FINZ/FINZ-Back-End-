package org.scoula.service.chatbot.handler.misc;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.scoula.util.chatbot.OpenAiClient;
import org.springframework.stereotype.Component;

/**
 * DefaultMessageHandler
 *
 * <p>IntentType.MESSAGE 에 해당하는 기본 메시지 처리 핸들러.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>사용자가 단순 대화 메시지를 입력했을 때 동작</li>
 *   <li>GPT(OpenAiClient)를 호출해 그대로 응답을 생성</li>
 *   <li>추천/분석 등 특별한 intent 로직이 필요 없는 경우 fallback 역할</li>
 * </ul>
 */
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
