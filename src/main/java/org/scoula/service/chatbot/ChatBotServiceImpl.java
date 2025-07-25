package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.scoula.domain.chatbot.dto.*;
import org.scoula.domain.chatbot.enums.ErrorType;

import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    // 기능별 메서드 정의해야함 .
    //    private ChatResponseDto handleProfileRecommendation(ChatRequestDto request) throws Exception {
    //    String prompt = promptBuilder.buildForProfile(String.valueOf(request.getUserId()));
    //    return callOpenAiAndBuildResponse(prompt, "RECOMMEND_PROFILE", request);
    //    }
    //
    //    private ChatResponseDto handleKeywordRecommendation(ChatRequestDto request) throws Exception {
    //    String prompt = promptBuilder.buildForKeyword(request.getMessage());
    //    return callOpenAiAndBuildResponse(prompt, "RECOMMEND_KEYWORD", request);
    //    }
    //
    //    private ChatResponseDto handleStockAnalysis(ChatRequestDto request) throws Exception {
    //    String prompt = promptBuilder.buildForAnalysis(request.getMessage());
    //    return callOpenAiAndBuildResponse(prompt, "ANALYZE_STOCK", request);
    //    }
    //
    //    private ChatResponseDto handleGeneralChat(ChatRequestDto request) throws Exception {
    //    return callOpenAiAndBuildResponse(request.getMessage(), "GENERAL", request);
    //    }

    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${openai.model}")
    private String model;
    
    // 쳇봇 mapper 주입
    private final ChatBotMapper chatBotMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ChatResponseDto getChatResponse(ChatRequestDto request) {
        try {
            // ====================== 1. 입력 데이터 추출 ======================
            String userMessage = request.getMessage();
            IntentType intentType = request.getIntentType();  // ex. "RECOMMEND_PROFILE"
            Integer userId = request.getUserId();
            Integer sessionId = request.getSessionId();



            if (intentType == null) {
                String prompt = buildIntentClassificationPrompt(userMessage);

                // GPT 호출
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", "user");
                msg.put("content", prompt);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("messages", List.of(msg));
                requestBody.put("temperature", 0);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(openaiApiKey);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);


                ResponseEntity<String> gptResponse = restTemplate.postForEntity(openaiApiUrl, entity, String.class);

                JsonNode json = objectMapper.readTree(gptResponse.getBody());
                String intentText = json.path("choices").get(0).path("message").path("content").asText().trim();

                try {
                    intentType = IntentType.valueOf(intentText); // enum 파싱
                } catch (IllegalArgumentException ex) {
                    // GPT 응답이 enum에 해당하지 않음 → fallback 처리
                    return handleError(
                            new IllegalArgumentException("의도 분류 실패: GPT 응답 = " + intentText),
                            userId,
                            IntentType.UNKNOWN
                    );
                }

                request.setIntentType(intentType);
            }

            // ========================2. 전처리======================
            // TODO: 민감 정보 마스킹 로직

            // 세션 관리 (intent 바뀌면 종료하고 새 세션 생성)
            if (sessionId == null) {
                // 세션이 없으면 새로 생성
                log.info("세션 생성... sessionId = {}", sessionId);
                ChatSessionDto newSession = ChatSessionDto.builder()
                        .userId(userId)
                        .lastIntent(intentType)
                        .build();
                chatBotMapper.insertChatSession(newSession);
                sessionId = newSession.getId();
            } else {
                // 기존 세션의 마지막 intent 가져옴
                log.info("기존 세션의 마지막 intent 가져옴... sessionId = {}", sessionId);
                IntentType lastIntent = chatBotMapper.getLastIntentBySessionId(sessionId);

                if (!intentType.equals(lastIntent)) {
                    // intent 바뀜 → 이전 세션 종료 + 새 세션 생성
                    log.info("세션 종료 시도: {}", sessionId);
                    chatBotMapper.endChatSession(sessionId);

                    ChatSessionDto newSession = ChatSessionDto.builder()
                            .userId(userId)
                            .lastIntent(intentType)
                            .build();
                    chatBotMapper.insertChatSession(newSession);
                    sessionId = newSession.getId();
                } else {
                    log.info("lastIntent만 갱신... sessionId = {}", sessionId);
                    // intent 같음 → lastIntent만 갱신
                    chatBotMapper.updateChatSessionIntent(ChatSessionDto.builder()
                            .id(sessionId)
                            .lastIntent(intentType)
                            .build());
                }
            }
            // ====================== 3. 의도 분류 ======================



            // ====================== 4. 사용자 메시지 저장 ======================
            // chat_messages 테이블에 사용자 메시지 저장
            saveChatMessage(userId, sessionId, "user", userMessage, intentType);
            
            // 에러 발생시 저장
            if (intentType == IntentType.ERROR && userMessage != null && !userMessage.trim().isEmpty()) {
                ErrorType errorType;
                ChatErrorDto errorDto = ChatErrorDto.builder()
                        .userId(userId)
                        .errorMessage(userMessage)  // 사용자가 입력한 내용 자체 저장
                        .errorType(ErrorType.GPT)
                        .build();
                chatBotMapper.insertChatError(errorDto);
            }



            // ====================== 5. OpenAI API 호출 ======================
            // GPT 메시지 포맷 구성
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", userMessage);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(message));
            body.put("temperature", 0.6);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(openaiApiUrl, entity, String.class);

            // ====================== 6. 응답 성공 여부 확인 ======================
            if (!response.getStatusCode().is2xxSuccessful()) {
                return handleError(new RuntimeException("OpenAI 응답 실패 - 상태코드: " + response.getStatusCodeValue()), userId, intentType);
            }

            // ====================== 7. 응답 파싱 ======================
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // ====================== 8. GPT 응답 저장 ======================
            // chat_messages 테이블에 GPT 응답 저장
            ChatMessageDto gptMessage = saveChatMessage(userId, sessionId, "assistant", content, intentType);

            // ====================== 9. 최종 응답 반환 ======================
            return ChatResponseDto.builder()
                    .content(content.trim())
                    .intentType(intentType)
                    .messageId(gptMessage.getId())
                    .sessionId(sessionId)
                    .build();

        } catch (Exception e) {
            return handleError(e, request.getUserId(), request.getIntentType() != null ? request.getIntentType() : IntentType.UNKNOWN);

        }
    }

    // ====================== 예외 처리 함수 ======================
    private ChatResponseDto handleError(Exception e, Integer userId, IntentType intentType) {
        log.error("OpenAI 호출 중 예외 발생", e);

        // chat_errors 테이블 저장

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


    // 메세지 저장 함수
    private ChatMessageDto saveChatMessage(Integer userId, Integer sessionId, String role, String content, IntentType intentType) {
        ChatMessageDto message = ChatMessageDto.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .intentType(intentType)
                .build();

        chatBotMapper.insertChatMessage(message); // insert 시 keyProperty="id"로 id 채워짐
        return message; // ID 포함된 message 반환
    }

    // 의도 분류 프롬프트
    private String buildIntentClassificationPrompt(String userMessage) {
        return """
    You are an intent classifier for a financial chatbot.

    Classify the user's message into one of the following intent types **based on the meaning**:

    - MESSAGE: General conversation or small talk.
    - RECOMMEND_PROFILE: Ask for stock recommendations based on investment profile.
    - RECOMMEND_KEYWORD: Ask for stock recommendations by keyword (e.g., AI-related stocks).
    - STOCK_ANALYZE: Ask for analysis of a specific stock (e.g., "Tell me about Samsung Electronics").
    - PORTFOLIO_ANALYZE: Ask to analyze the user's mock investment performance.
    - SESSION_END: Wants to end the conversation.
    - ERROR: Clear error or invalid message.
    - UNKNOWN: Cannot determine intent.

    Just return the intent type only, no explanation.

    Example 1:
    User: "AI 관련된 주식 추천해줘"
    Answer: RECOMMEND_KEYWORD

    Example 2:
    User: "삼성전자 분석해줘"
    Answer: STOCK_ANALYZE
    
    Example 3:
    User: "가치주 추천"
    Answer: RECOMMEND_KEYWORD

    User: %s
    """.formatted(userMessage);
    }

}
