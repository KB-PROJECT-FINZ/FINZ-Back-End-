package org.scoula.service.chatbot.handler.context;

import lombok.Builder;
import lombok.Getter;
import org.scoula.domain.chatbot.enums.IntentType;

/**
 * ExecutionContext
 *
 * <p>의도별 핸들러가 실행될 때 필요한 공통 실행 맥락(Context)을 담는 DTO.
 * - ChatBotServiceImpl에서 사용자 입력과 세션 정보를 모아 생성
 * - IntentHandler가 실제 로직을 수행할 때 이 객체를 참조</p>
 *
 * <h3>주요 역할</h3>
 * <ul>
 *   <li>현재 대화의 사용자/세션/메시지 식별자 제공</li>
 *   <li>사용자가 입력한 메시지와 분류된 의도 전달</li>
 *   <li>핸들러가 공통적으로 필요로 하는 최소 데이터 보장</li>
 * </ul>
 */
@Getter
@Builder
public class ExecutionContext {
    private final Integer userId;
    private final Integer sessionId;
    private final String userMessage;
    private Integer messageId;
    private final IntentType intentType;
}