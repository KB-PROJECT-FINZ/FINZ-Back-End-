package org.scoula.service.chatbot.handler;

import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;


/**
 * IntentHandler
 *
 * <p>챗봇 Intent 처리용 공통 인터페이스.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>각 IntentType(예: RECOMMEND_PROFILE, STOCK_ANALYZE 등)에 대응하는 핸들러 구현</li>
 *   <li>ExecutionContext를 입력받아 ExecutionResult를 반환</li>
 *   <li>핸들러 간 일관된 구조 보장 (전략 패턴 구조)</li>
 * </ul>
 *
 * <h3>메서드</h3>
 * <ul>
 *   <li><b>supports()</b> : 어떤 IntentType을 지원하는지 반환</li>
 *   <li><b>handle(ctx)</b> : 실제 Intent 처리 로직 실행, 결과(ExecutionResult) 반환</li>
 * </ul>
 *
 * <h3>예시</h3>
 * <pre>{@code
 * @Component
 * public class RecommendProfileHandler implements IntentHandler {
 *     @Override
 *     public IntentType supports() { return IntentType.RECOMMEND_PROFILE; }
 *
 *     @Override
 *     public ExecutionResult handle(ExecutionContext ctx) {
 *         // 처리 로직
 *         return ExecutionResult.builder().finalContent("추천 결과").build();
 *     }
 * }
 * }</pre>
 */
public interface IntentHandler {

    /** 이 핸들러가 지원하는 IntentType */
    IntentType supports();

    /** 실행 컨텍스트 기반 Intent 처리 후 결과 반환 */
    ExecutionResult handle(ExecutionContext ctx) throws Exception;
}