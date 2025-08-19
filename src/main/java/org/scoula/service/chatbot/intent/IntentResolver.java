package org.scoula.service.chatbot.intent;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.PromptBuilder;
import org.scoula.util.chatbot.OpenAiClient;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class IntentResolver {

    private final OpenAiClient openAiClient;
    private final PromptBuilder promptBuilder;

    // 프론트가 intent를 주면 그대로 사용. 없거나 MESSAGE면 GPT로 분류
    public IntentType resolve(String userMessage, IntentType incoming) {
        if (incoming != null && incoming != IntentType.MESSAGE) {
            log.info("[INTENT] 프론트 지정 → {}", incoming);
            return incoming;
        }

        if (userMessage == null || userMessage.isBlank()) {
            log.warn("[INTENT] 빈 메시지 → UNKNOWN");
            return IntentType.UNKNOWN;
        }

        String prompt = promptBuilder.buildIntentClassificationPrompt(userMessage);
        String intentText;
        try {
            intentText = openAiClient.getChatCompletion(prompt);
        } catch (Exception e) {
            log.error("[INTENT] GPT 호출 실패: {}", e.getMessage(), e);
            return IntentType.UNKNOWN;
        }


        try {
            IntentType resolved = IntentType.valueOf(intentText == null ? "" : intentText.trim().toUpperCase());
            log.info("[INTENT] GPT 분류 → {}", resolved);
            return applyHeuristics(userMessage, resolved);

        } catch (IllegalArgumentException ex) {
            log.warn("[INTENT] GPT 응답 enum 파싱 실패: '{}'", intentText);
            return IntentType.UNKNOWN;
        }
    }

    // 보정
    private IntentType applyHeuristics(String msg, IntentType base) {
        if (base == IntentType.UNKNOWN) {
            String m = msg.toLowerCase();
            if (m.contains("포트폴리오")) return IntentType.PORTFOLIO_ANALYZE;
            if (m.contains("용어") || m.contains("설명")) return IntentType.TERM_EXPLAIN;
        }
        return base;
    }

}
