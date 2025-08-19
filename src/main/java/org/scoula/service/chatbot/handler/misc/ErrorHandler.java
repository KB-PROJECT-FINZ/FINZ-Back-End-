package org.scoula.service.chatbot.handler.misc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.ChatErrorDto;
import org.scoula.domain.chatbot.enums.ErrorType;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.springframework.stereotype.Component;

/**
 * ErrorHandler
 *
 * <p>IntentType.ERROR 상황에서 실행되는 전용 핸들러.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>예상치 못한 오류가 발생했을 때 실행</li>
 *   <li>사용자의 입력(혹은 오류 상황)을 에러 로그 테이블에 저장</li>
 *   <li>사용자에게는 안전한 안내 메시지를 반환</li>
 * </ul>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ErrorHandler implements IntentHandler {

    private final ChatBotMapper chatBotMapper; // 선택: 에러 저장 안 할거면 제거해도 됨

    @Override
    public IntentType supports() {
        return IntentType.ERROR;
    }

    @Override
    public ExecutionResult handle(ExecutionContext ctx) {
        // (옵션) 사용자 입력을 에러 테이블에 기록
        try {
            String msg = ctx.getUserMessage();
            if (msg != null && !msg.isBlank() && ctx.getUserId() != null) {
                ChatErrorDto errorDto = ChatErrorDto.builder()
                        .userId(ctx.getUserId())
                        .errorMessage(msg)            // 필요시 마스킹 고려
                        .errorType(ErrorType.ETC)     // 상황에 맞게 API/DB/GPT 등으로 구분 가능
                        .build();
                chatBotMapper.insertChatError(errorDto);
            }
        } catch (Exception logEx) {
            log.warn("[ErrorHandler] error logging failed: {}", logEx.getMessage());
        }

        // 최종 사용자 안내 메시지
        String finalText = "⚠ 시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        return ExecutionResult.builder()
                .finalContent(finalText)
                .build();
    }
}