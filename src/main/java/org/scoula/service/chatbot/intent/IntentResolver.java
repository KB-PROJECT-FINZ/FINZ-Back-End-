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
        String prompt = promptBuilder.buildIntentClassificationPrompt(userMessage);
        String intentText = openAiClient.getChatCompletion(prompt);
        try {
            IntentType resolved = IntentType.valueOf(intentText);
            log.info("[INTENT] GPT 분류 → {}", resolved);
            return resolved;
        } catch (IllegalArgumentException ex) {
            log.warn("[INTENT] GPT 응답을 enum으로 파싱 실패: {}", intentText);
            return IntentType.UNKNOWN;
        }
    }

}
