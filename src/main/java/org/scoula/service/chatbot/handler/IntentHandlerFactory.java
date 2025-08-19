package org.scoula.service.chatbot.handler;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.enums.IntentType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IntentHandlerFactory {
    private final List<IntentHandler> handlers;

    public IntentHandler get(IntentType type) {
        return handlers.stream()
                .filter(h -> h.supports() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported intent: " + type));
    }
}
