package org.scoula.service.chatbot.handler;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IntentHandlerFactory
 *
 * <p>IntentHandler 구현체들을 모아두고,
 * 요청된 IntentType에 맞는 핸들러를 찾아주는 팩토리 클래스.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>Spring DI(@Component)로 모든 IntentHandler 구현체를 리스트로 주입</li>
 *   <li>요청된 IntentType과 일치하는 핸들러를 검색 후 반환</li>
 *   <li>지원하지 않는 IntentType일 경우 예외 발생</li>
 * </ul>
 *
 * <h3>동작 방식</h3>
 * <ol>
 *   <li>List<IntentHandler> 에 모든 핸들러 구현체가 자동 주입됨</li>
 *   <li>get(IntentType type) 호출 시 → handlers.stream() 으로 필터링</li>
 *   <li>supports() == type 인 핸들러 반환</li>
 *   <li>없으면 IllegalArgumentException 발생</li>
 * </ol>
 *
 * <h3>예시</h3>
 * <pre>{@code
 * IntentHandler handler = intentHandlerFactory.get(IntentType.RECOMMEND_PROFILE);
 * ExecutionResult result = handler.handle(ctx);
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class IntentHandlerFactory {
    /** 등록된 모든 IntentHandler 구현체 (Spring DI) */
    private final List<IntentHandler> handlers;

    /**
     * 주어진 IntentType을 처리할 수 있는 핸들러 반환
     *
     * @param type IntentType (예: RECOMMEND_PROFILE, STOCK_ANALYZE 등)
     * @return 해당 IntentHandler 구현체
     * @throws IllegalArgumentException 지원하지 않는 타입이면 예외 발생
     */
    public IntentHandler get(IntentType type) {
        return handlers.stream()
                .filter(h -> h.supports() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported intent: " + type));
    }
}
