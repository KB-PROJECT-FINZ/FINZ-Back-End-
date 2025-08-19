package org.scoula.service.chatbot.handler.misc;

import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.springframework.stereotype.Component;

/**
 * UnknownHandler
 *
 * <p>IntentType.UNKNOWN 상황을 처리하는 전용 핸들러.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>의도(Intent)를 정확히 분류하지 못했을 때 실행된다.</li>
 *   <li>사용자에게 “이해하지 못했다”는 안내 메시지를 반환한다.</li>
 *   <li>대화의 맥락이 불명확하거나 GPT 분류가 실패한 경우 fallback 처리 역할.</li>
 * </ul>
 *
 */
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