package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.ChatRequestDto;
import org.scoula.domain.chatbot.dto.ChatResponseDto;
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

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${openai.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ChatResponseDto getChatResponse(ChatRequestDto request) {
        try {
            // ChatRequestDto에서 필요한 파라미터 추출
            String userMessage = request.getMessage();
            String intentType = request.getIntentType();  // ex. "RECOMMEND_PROFILE"
            Integer userId = request.getUserId();
            Integer sessionId = request.getSessionId();

            // TODO: 민감 정보 마스킹 로직
            // TODO: 세션 조회 또는 생성 로직 (sessionId가 없으면 새로 생성)
            // TODO: intentType이 없는 경우 → intent 분류 시도
            // if (intentType == null) {
            //     intentType = classifyIntent(userMessage); // 분류 함수 추후 구현
            // }

            // 분류함수 하드코딩 -> 나중에 GPT로 바꿔야함
//            private String classifyIntent(String message) {
//                if (message.contains("추천")) return "RECOMMEND_PROFILE";
//                else if (message.contains("분석")) return "ANALYZE_STOCK";
//                else return null; // 분류 실패
//            }



            // TODO: intent 분류 실패 시 fallback 처리 및 chat_errors 저장
//            if (intentType == null) {
//                return handleError(
//                        new IllegalArgumentException("의도 분류 실패: fallback 응답 처리"),
//                        userId,
//                        "UNKNOWN_INTENT"
//                );
//            }


            // TODO: chat_messages 테이블에 사용자 메시지 저장
            // saveChatMessage(userId, sessionId, "user", userMessage, intentType);

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

            // 응답 성공 여부 체크
            if (!response.getStatusCode().is2xxSuccessful()) {
                return handleError(new RuntimeException("OpenAI 응답 실패 - 상태코드: " + response.getStatusCodeValue()), userId, intentType);
            }

            // 응답 파싱
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // TODO: chat_messages 테이블에 GPT 응답 저장
            // saveChatMessage(userId, sessionId, "assistant", content, intentType);

            // TODO: 세션에 마지막 intent 저장 + 세션 종료 조건 검사 및 update

            return ChatResponseDto.builder()
                    .content(content.trim())
                    .intentType(intentType)
                    .build();

        } catch (Exception e) {
            return handleError(e, request.getUserId(), request.getIntentType());
        }
    }

    // 에러 핸들링 분리 함수
    private ChatResponseDto handleError(Exception e, Integer userId, String intentType) {
        log.error("OpenAI 호출 중 예외 발생", e);

        // TODO: chat_errors 테이블 저장


        return ChatResponseDto.builder()
                .content("⚠ 서버 오류가 발생했습니다. 다시 시도해주세요.")
                .intentType("ERROR")
                .build();
    }
}
