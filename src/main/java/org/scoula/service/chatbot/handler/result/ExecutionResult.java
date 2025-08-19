package org.scoula.service.chatbot.handler.result;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

/**
 * ExecutionResult
 *
 * <p>IntentHandler 실행 결과를 래핑하는 DTO.</p>
 *
 * <h3>구성 요소</h3>
 * <ul>
 *   <li><b>finalContent</b> : 최종 사용자 응답 텍스트</li>
 *   <li><b>requestedPeriod</b> : 요청된 기간(예: N일, N개월). 없으면 null</li>
 *   <li><b>extras</b> : 부가 데이터(Map) - 핸들러별 필요시 확장</li>
 * </ul>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>@Builder로 편리한 빌더 패턴 제공</li>
 *   <li>Optional 헬퍼 메서드(requestedPeriodOpt)로 Null-safety 확보</li>
 * </ul>
 */
@Getter
@Builder
public class ExecutionResult {
    private final String finalContent;
    private final Integer requestedPeriod;
    private final Map<String, Object> extras;

    /**
     * requestedPeriod를 Optional로 안전하게 반환.
     * <p>
     * → null 체크 대신 Optional 활용 가능
     * </p>
     */
    public Optional<Integer> requestedPeriodOpt() {
        return Optional.ofNullable(requestedPeriod);
    }
}
