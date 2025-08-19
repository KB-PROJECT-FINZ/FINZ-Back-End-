package org.scoula.service.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.scoula.service.chatbot.intent.IntentResolver;
import org.scoula.service.chatbot.message.MessageService;
import org.scoula.service.chatbot.session.ChatSessionService;
import org.scoula.util.chatbot.*;
import org.scoula.domain.chatbot.dto.*;
import org.scoula.domain.chatbot.enums.ErrorType;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.springframework.stereotype.Service;

/**
 * ChatBotServiceImpl
 *
 * <p>챗봇의 메인 레이어.
 * 사용자 메시지를 입력  → 의도(Intent) 분류 → 세션 관리 → 메시지 영속화 → 의도별 핸들러 실행 → 최종 응답
 *
 * <h3>핵심 흐름</h3>
 * <ol>
 *   <li>요청 파싱 및 기본 값 확보</li>
 *   <li>(필요 시) GPT 기반 의도 분류 수행</li>
 *   <li>IntentResolver로 최종 의도 확정</li>
 *   <li>세션 관리: intent 변경 시 기존 세션 종료 및 새 세션 보장</li>
 *   <li>사용자 메시지 DB 저장</li>
 *   <li>의도별 핸들러 라우팅 & 실행</li>
 *   <li>어시스턴트 메시지 DB 저장</li>
 *   <li>최종 응답 DTO 생성 & 반환</li>
 * </ol>
 *
 * <h3>예외 처리</h3>
 * - 외부 API, DB, GPT 관련 예외를 구분하여 ErrorType으로 기록하고, 세션 정리 후 안전한 메시지 반환.
 *
 * <h3>주의 사항</h3>
 * - GPT 의도 분류는 IntentType enum 이름과 정확히 매칭되는 문자열 (예: "RECOMMEND_KEYWORD").
 * - 동일 응답의 중복 저장 방지.
 * - 세션 종료/생성과 메시지 저장의 순서 보장.
 */


@Log4j2
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    // ====== 의존성 주입: Lombok @RequiredArgsConstructor 로 생성자 주입 ======
    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;
    private final ChatBotMapper chatBotMapper;
    private final IntentResolver intentResolver;
    private final MessageService messageService;
    private final org.scoula.service.chatbot.handler.IntentHandlerFactory handlerFactory;
    private final ChatSessionService chatSessionService;


    @Override
    public ChatResponseDto getChatResponse(ChatRequestDto request) {
        try {
            // ====================== 1. 입력 데이터 추출 ======================
            final String userMessage = request.getMessage();
            IntentType intentType = request.getIntentType();
            final Integer userId = request.getUserId();
            Integer sessionId = request.getSessionId();

            log.info("[INTENT] 초기 intentType: {}", intentType);
            log.info("✅ 프론트에서 intentType 명시 → GPT 분류 생략: {}", intentType);

            // ====================== 2) 의도 확정(단일 진입점) ======================
            // - 프론트가 준 intent가 있으면 그대로 사용
            // - 없거나 MESSAGE면 IntentResolver 내부에서 GPT 분류 수행
            final IntentType before = intentType;
            intentType = intentResolver.resolve(userMessage, intentType);
            request.setIntentType(intentType);

            if (before != intentType) {
                log.info("[INTENT] 의도 변경: {} -> {}", before, intentType);
            } else {
                log.info("[INTENT] 의도 유지: {}", intentType);
            }

            // ====================== 3) 세션 관리 ======================
            // intent 바뀌면 종료 후 새 세션 생성
            sessionId = chatSessionService.ensureSession(userId, sessionId, intentType);

            // ====================== 4. 사용자 메시지 저장 ======================
            // chat_messages 테이블에 사용자 메시지 저장
            ChatMessageDto userMsg = messageService.save(userId, sessionId, "user", userMessage, intentType);
            Integer messageId = (userMsg != null ? userMsg.getId() : null);
            log.info("[MESSAGE] 사용자 메시지 저장 완료 → id={}", messageId);

            // ====================== 5. 핸들러 실행 컨텍스트 구성 ======================
            // 의도별 핸들러가 필요한 모든 정보를 담은 ExecutionContext를 빌드.
            ExecutionContext ctx = ExecutionContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .messageId(messageId)
                    .userMessage(userMessage)
                    .intentType(intentType)
                    .build();

            // ====================== 6. 의도별 라우팅 결정 ======================
            // intentType 자체가 라우팅 키
            IntentType route = switch (intentType) {
                case RECOMMEND_PROFILE, RECOMMEND_KEYWORD, STOCK_ANALYZE, PORTFOLIO_ANALYZE,
                     TERM_EXPLAIN, SESSION_END, UNKNOWN, MESSAGE, ERROR -> intentType;
            };

            // ====================== 7. 핸들러 실행 ======================
            // 의도에 해당하는 핸들러를 팩토리에서 가져와 실행.
            ExecutionResult result = handlerFactory.get(route).handle(ctx);
            String finalResponse = result.getFinalContent();

            // ====================== 8. 어시스턴트 메시지 저장 ======================
            ChatMessageDto gptMessage =
                    messageService.save(userId, sessionId, "assistant", finalResponse, intentType);


            // ====================== 9. 최종 응답 반환 ======================
            // 클라이언트에서 후속 요청에 활용할 수 있도록 messageId, sessionId, intentType 포함.
            return ChatResponseDto.builder()
                    .content(finalResponse.trim())
                    .intentType(intentType)
                    .messageId(gptMessage != null ? gptMessage.getId() : null)
                    .sessionId(sessionId)
                    .requestedPeriod(result.getRequestedPeriod()) // <-- 핸들러가 세팅했다면 전달, 아니면 null
                    .build();

        } catch (Exception e) {
            // 모든 예외는 공통 에러 핸들러로 위임하여
            // - 세션 정리
            // - 에러 기록(DB)
            // - 사용자 안전 메시지 반환
            return handleError(e, request.getUserId(), request.getIntentType() != null ? request.getIntentType() : IntentType.UNKNOWN);
        }
    }

    /**
     * 공통 예외 처리.
     * <ul>
     *   <li>가능하면 현재 세션 정리(종료)</li>
     *   <li>에러 유형 분류(ErrorType) 및 DB 기록</li>
     *   <li>사용자에게는 일반화된 경고 메시지 반환</li>
     * </ul>
     *
     * @param e          발생한 예외
     * @param userId     사용자 ID (에러 기록용)
     * @param intentType 에러 발생 시점의 의도 (세션 종료 판단 용도)
     * @return 에러용 ChatResponseDto
     */
    // ====================== 예외 처리 함수 ======================
    private ChatResponseDto handleError(Exception e, Integer userId, IntentType intentType) {
        log.error("[ERROR] OpenAI 호출 중 예외 발생", e);

        // 1) 세션 정리 시도: ERROR 상태가 아닌 경우에만 종료 시도
        try {
            if (intentType != null && intentType != IntentType.ERROR) {
                chatSessionService.endActiveSessionIfAny(userId);
            }
        } catch (Exception sessionEx) {
            // 세션 종료 자체가 실패하더라도 서비스 중단 없이 경고 로그만 남김
            log.warn("[SESSION] 에러 발생 시 세션 종료 실패: {}", sessionEx.getMessage());
        }

        // 2) 에러 타입 분류: API/DB/GPT/기타
        //    - 메시지 내용 검사로 구분하는 부분은 향후 전용 예외 타입 도입 시 개선 가능(확장)
        ErrorType errorType;
        if (e instanceof org.springframework.web.client.RestClientException) {
            errorType = ErrorType.API;
        } else if (e instanceof java.sql.SQLException || e.getMessage().contains("MyBatis")) {
            errorType = ErrorType.DB;
        } else if (e.getMessage().contains("OpenAI")) {
            errorType = ErrorType.GPT;
        } else {
            errorType = ErrorType.ETC;
        }

        // 3) 에러 기록
        ChatErrorDto errorDto = ChatErrorDto.builder()
                .userId(userId)
                .errorMessage(e.getMessage())
                .errorType(errorType)
                .build();

        chatBotMapper.insertChatError(errorDto);

        // 4) 사용자에게 에러 메시지 반환
        return ChatResponseDto.builder()
                .content("⚠ 서버 오류가 발생했습니다. 다시 시도해주세요.")
                .intentType(IntentType.ERROR)
                .build();
    }
}

