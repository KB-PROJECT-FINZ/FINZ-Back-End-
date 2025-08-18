package org.scoula.service.chatbot.handler.result;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

@Getter
@Builder
public class ExecutionResult {
    private final String finalContent;
    private final Integer requestedPeriod;   // 없으면 null
    private final Map<String, Object> extras;

    public Optional<Integer> requestedPeriodOpt() {
        return Optional.ofNullable(requestedPeriod);
    }
}
