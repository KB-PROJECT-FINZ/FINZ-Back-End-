package org.scoula.service.chatbot.handler.misc;

import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.springframework.stereotype.Component;

/**
 * SessionEndHandler
 *
 * <p>IntentType.SESSION_END 에 대한 전용 핸들러.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>사용자가 "대화 종료"를 요청했을 때 종료 안내 메시지를 반환한다.</li>
 *   <li>(선택) 실제 세션 종료 처리는 상위 서비스(ChatBotServiceImpl.ensureSession/세션 서비스)에서 수행되며,
 *       필요 시 이 핸들러에서 세션 종료 API를 호출하도록 확장할 수 있다.</li>
 * </ul>
 *
 */
@Component
// SessionEnd
public class SessionEndHandler implements IntentHandler {
    @Override public IntentType supports() { return IntentType.SESSION_END; }
    @Override public ExecutionResult handle(ExecutionContext ctx) {
        return ExecutionResult.builder().finalContent("대화를 종료합니다. 감사합니다.").build();
    }
}