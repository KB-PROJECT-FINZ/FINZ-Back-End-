package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.ChatMessageDto;
import org.scoula.domain.chatbot.dto.ChatRequestDto;
import org.scoula.domain.chatbot.dto.ChatResponseDto;
import org.scoula.domain.chatbot.dto.ChatSessionDto;
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

            // ========================2. 전처리======================
            // TODO: 민감 정보 마스킹 로직

            // 세션 관리 (intent 바뀌면 종료하고 새 세션 생성)
            if (sessionId == null) {
                // 세션이 없으면 새로 생성
                ChatSessionDto newSession = ChatSessionDto.builder()
                        .userId(userId)
                        .lastIntent(intentType)
                        .build();
                chatBotMapper.insertChatSession(newSession);
                sessionId = newSession.getId();
            } else {
                // 기존 세션의 마지막 intent 가져옴
                IntentType lastIntent = chatBotMapper.getLastIntentBySessionId(sessionId);

                if (!intentType.equals(lastIntent)) {
                    // intent 바뀜 → 이전 세션 종료 + 새 세션 생성
                    chatBotMapper.endChatSession(sessionId); // ended_at 업데이트용 쿼리 필요

                    ChatSessionDto newSession = ChatSessionDto.builder()
                            .userId(userId)
                            .lastIntent(intentType)
                            .build();
                    chatBotMapper.insertChatSession(newSession);
                    sessionId = newSession.getId();
                } else {
                    // intent 같음 → lastIntent만 갱신
                    chatBotMapper.updateChatSessionIntent(ChatSessionDto.builder()
                            .id(sessionId)
                            .lastIntent(intentType)
                            .build());
                }
            }
            // ====================== 3. 의도 분류 ======================
            // TODO: intentType이 없는 경우 → intent 분류 시도  //null
            // if (intentType == null) {
                // TODO: GPT를 활용한 의도 분류 시도 or 하드코딩 분류
            //    intentType = classifyIntent(userMessage);
            //}

            // 분류함수 하드코딩 -> 나중에 GPT로 바꿔야함
//                  private String classifyIntent(String message) {
//                        if (message.contains("추천")) return "RECOMMEND_PROFILE";
//                        else if (message.contains("분석")) return "ANALYZE_STOCK";
//                        else return null; // 분류 실패
//                  }


            // TODO: intent 분류 실패 시 fallback 처리 및 chat_errors 저장

            //    if (intentType == null) {
            //            return handleError(
            //                    new IllegalArgumentException("의도 분류 실패: fallback 응답 처리"),
            //                    userId,
            //                    "UNKNOWN_INTENT"
            //            );
            //        }

            // ====================== 4. 사용자 메시지 저장 ======================
            // TODO: chat_messages 테이블에 사용자 메시지 저장
            saveChatMessage(userId, sessionId, "user", userMessage, intentType);

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
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(openaiApiUrl, entity, String.class);

            // ====================== 6. 응답 성공 여부 확인 ======================
            if (!response.getStatusCode().is2xxSuccessful()) {
                return handleError(new RuntimeException("OpenAI 응답 실패 - 상태코드: " + response.getStatusCodeValue()), userId, intentType);
            }

            // ====================== 7. 응답 파싱 ======================
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // ====================== 8. GPT 응답 저장 ======================
            // TODO: chat_messages 테이블에 GPT 응답 저장
            ChatMessageDto gptMessage = saveChatMessage(userId, sessionId, "assistant", content, intentType);


            // TODO: 세션에 마지막 intent 저장 + 세션 종료 조건 검사 및 update


            // ====================== 9. 최종 응답 반환 ======================
            return ChatResponseDto.builder()
                    .content(content.trim())
                    .intentType(intentType)
                    .messageId(gptMessage.getId())
                    .build();

        } catch (Exception e) {
            return handleError(e, request.getUserId(), request.getIntentType());
        }
    }

    // ====================== 예외 처리 함수 ======================
    private ChatResponseDto handleError(Exception e, Integer userId, IntentType intentType) {
        log.error("OpenAI 호출 중 예외 발생", e);

        // TODO: chat_errors 테이블 저장


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

}
