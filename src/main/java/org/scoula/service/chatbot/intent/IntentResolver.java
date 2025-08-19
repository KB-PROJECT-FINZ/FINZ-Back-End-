package org.scoula.service.chatbot.intent;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.PromptBuilder;
import org.scoula.util.chatbot.OpenAiClient;
import org.springframework.stereotype.Component;

/**
 * IntentResolver
 *
 * <p>사용자 입력 메시지를 기반으로 최종 IntentType을 결정하는 역할.</p>
 *
 * <h3>전체 흐름</h3>
 * <ol>
 *   <li>프론트엔드에서 intent를 지정해주면 그대로 사용</li>
 *   <li>프론트 지정 intent가 없거나 MESSAGE일 경우 → GPT 기반 분류</li>
 *   <li>빈 메시지는 UNKNOWN 처리</li>
 *   <li>GPT 호출 실패 → UNKNOWN 처리</li>
 *   <li>GPT 결과를 enum으로 매핑 실패 시 → UNKNOWN 처리</li>
 *   <li>보정 로직(applyHeuristics)으로 일부 UNKNOWN → 의미 있는 intent로 교정</li>
 * </ol>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>GPT 응답 기반 IntentType 매핑</li>
 *   <li>의미 없는 응답에 대비한 fallback(UNKNOWN)</li>
 *   <li>포트폴리오, 용어설명 등 특정 키워드 기반 보정</li>
 * </ul>
 *
 * <h3>예시</h3>
 * <pre>{@code
 * IntentType resolved = intentResolver.resolve("포트폴리오 분석해줘", null);
 * // → PORTFOLIO_ANALYZE
 * }</pre>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class IntentResolver {

    private final OpenAiClient openAiClient;
    private final PromptBuilder promptBuilder;

    /**
     * 사용자 메시지와 프론트엔드 제공 intent를 기반으로 최종 IntentType 결정
     *
     * @param userMessage 사용자 입력 메시지
     * @param incoming 프론트에서 지정한 IntentType (nullable)
     * @return 최종 IntentType
     */
    public IntentType resolve(String userMessage, IntentType incoming) {
        // 1) 프론트에서 intent가 넘어온 경우 그대로 사용
        if (incoming != null && incoming != IntentType.MESSAGE) {
            log.info("[INTENT] 프론트 지정 → {}", incoming);
            return incoming;
        }

        // 2) 메시지가 비어있으면 UNKNOWN
        if (userMessage == null || userMessage.isBlank()) {
            log.warn("[INTENT] 빈 메시지 → UNKNOWN");
            return IntentType.UNKNOWN;
        }

        // 3) GPT로 intent 분류
        String prompt = promptBuilder.buildIntentClassificationPrompt(userMessage);
        String intentText;
        try {
            intentText = openAiClient.getChatCompletion(prompt);
        } catch (Exception e) {
            log.error("[INTENT] GPT 호출 실패: {}", e.getMessage(), e);
            return IntentType.UNKNOWN;
        }

        // 4) GPT 응답 → Enum 매핑
        try {
            IntentType resolved = IntentType.valueOf(intentText == null ? "" : intentText.trim().toUpperCase());
            log.info("[INTENT] GPT 분류 → {}", resolved);
            return applyHeuristics(userMessage, resolved);

        } catch (IllegalArgumentException ex) {
            log.warn("[INTENT] GPT 응답 enum 파싱 실패: '{}'", intentText);
            return IntentType.UNKNOWN;
        }
    }

    /**
     * Heuristic 보정 로직
     * <p>
     * GPT가 UNKNOWN 반환했을 경우 → 특정 키워드 기반으로 intent 교정
     * </p>
     */
    private IntentType applyHeuristics(String msg, IntentType base) {
        if (base == IntentType.UNKNOWN) {
            String m = msg.toLowerCase();
            if (m.contains("포트폴리오")) return IntentType.PORTFOLIO_ANALYZE;
            if (m.contains("용어") || m.contains("설명")) return IntentType.TERM_EXPLAIN;
        }
        return base;
    }

}
