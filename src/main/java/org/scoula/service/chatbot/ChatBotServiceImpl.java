package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
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


@Log4j2
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;
    private final ChatBotMapper chatBotMapper;
    private final ObjectMapper objectMapper;
    private final IntentResolver intentResolver;
    private final MessageService messageService;
    private final org.scoula.service.chatbot.handler.IntentHandlerFactory handlerFactory;


    // 세션
    private final ChatSessionService chatSessionService;

    @Override
    public ChatResponseDto getChatResponse(ChatRequestDto request) {
        try {
            // ====================== 1. 입력 데이터 추출 ======================
            String userMessage = request.getMessage();
            IntentType intentType = request.getIntentType();
            Integer userId = request.getUserId();
            Integer sessionId = request.getSessionId();

            log.info("[INTENT] 초기 intentType: {}", intentType);

            if (intentType == null || intentType == IntentType.MESSAGE) {
                String prompt = promptBuilder.buildIntentClassificationPrompt(userMessage);

                // GPT 호출
                String intentText = openAiClient.getChatCompletion(prompt);
                log.info("[INTENT] GPT 의도 분류 요청 프롬프트 생성 완료");

                try {
                    intentType = IntentType.valueOf(intentText); // enum 파싱
                    log.info("[INTENT] GPT 의도 분류 결과 → intentType: {}", intentType);

                } catch (IllegalArgumentException ex) {
                    // GPT 응답이 enum에 해당하지 않음 → fallback 처리
                    return handleError(
                            new IllegalArgumentException("의도 분류 실패: GPT 응답 = " + intentText),
                            userId,
                            IntentType.UNKNOWN
                    );
                }
                request.setIntentType(intentType); // 이후 로직을 위해 저장
            } else {
                log.info("✅ 프론트에서 intentType 명시 → GPT 분류 생략: {}", intentType);
            }
            // 의도 분류
            intentType = intentResolver.resolve(userMessage, intentType);
            request.setIntentType(intentType);

            // 세션 관리 (intent 바뀌면 종료하고 새 세션 생성)
            sessionId = chatSessionService.ensureSession(userId, sessionId, intentType);

            // ====================== 4. 사용자 메시지 저장 ======================
            // chat_messages 테이블에 사용자 메시지 저장
            messageService.save(userId, sessionId, "user", userMessage, intentType);
            log.info("[MESSAGE] 사용자 메시지 저장 완료");

            // 핸들러 실행
            ExecutionContext ctx = ExecutionContext.builder()
                    .userId(userId).sessionId(sessionId)
                    .userMessage(userMessage).intentType(intentType).build();

            IntentType route = switch (intentType) {
                case RECOMMEND_PROFILE, RECOMMEND_KEYWORD, STOCK_ANALYZE, PORTFOLIO_ANALYZE,
                     TERM_EXPLAIN, SESSION_END, UNKNOWN, MESSAGE, ERROR -> intentType;
            };

            ExecutionResult result = handlerFactory.get(route).handle(ctx);
            String finalResponse = result.getFinalContent();

            // 어시스턴트 메시지 중복 없이 저장
            ChatMessageDto gptMessage =
                    messageService.save(userId, sessionId, "assistant", finalResponse, intentType);


            // ====================== 9. 최종 응답 반환 ======================
            return ChatResponseDto.builder()
                    .content(finalResponse.trim())
                    .intentType(intentType)
                    .messageId(gptMessage != null ? gptMessage.getId() : null)
                    .sessionId(sessionId)
                    .requestedPeriod(result.getRequestedPeriod()) // <-- 핸들러가 세팅했다면 전달, 아니면 null
                    .build();

        } catch (Exception e) {
            return handleError(e, request.getUserId(), request.getIntentType() != null ? request.getIntentType() : IntentType.UNKNOWN);
        }
    }

    // ====================== 예외 처리 함수 ======================
    private ChatResponseDto handleError(Exception e, Integer userId, IntentType intentType) {
        log.error("[ERROR] OpenAI 호출 중 예외 발생", e);

        try {
            if (intentType != null && intentType != IntentType.ERROR) {
                chatSessionService.endActiveSessionIfAny(userId);
            }
        } catch (Exception sessionEx) {
            log.warn("[SESSION] 에러 발생 시 세션 종료 실패: {}", sessionEx.getMessage());
        }

        // 에러 타입 분기
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

        ChatErrorDto errorDto = ChatErrorDto.builder()
                .userId(userId)
                .errorMessage(e.getMessage())
                .errorType(errorType)
                .build();

        chatBotMapper.insertChatError(errorDto);

        return ChatResponseDto.builder()
                .content("⚠ 서버 오류가 발생했습니다. 다시 시도해주세요.")
                .intentType(IntentType.ERROR)
                .build();
    }

}

