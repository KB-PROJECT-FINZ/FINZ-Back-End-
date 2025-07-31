package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.util.chatbot.ProfileStockFilter;
import org.scoula.api.mocktrading.VolumeRankingApi;
import org.scoula.domain.chatbot.dto.*;
import org.scoula.domain.chatbot.enums.ErrorType;

import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.scoula.util.chatbot.ChatAnalysisMapper;
import org.scoula.util.chatbot.ProfileStockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    private final RestTemplate restTemplate;

    private final PromptBuilder promptBuilder;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${openai.api.model}")
    private String model;

    // 성향에 따른 종목 추천 유틸
    @Autowired
    private ProfileStockRecommender profileStockRecommender;

    // 모의투자팀이 열심히 만든~ 볼륨랭킹
    @Autowired
    private VolumeRankingApi volumeRankingApi;

    @Autowired
    private UserProfileService userProfileService;

    // 쳇봇 mapper 주입
    private final ChatBotMapper chatBotMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ChatResponseDto getChatResponse(ChatRequestDto request) {
        try {
            // ====================== 1. 입력 데이터 추출 ======================
            String userMessage = request.getMessage();
            IntentType intentType = request.getIntentType();
            Integer userId = request.getUserId();
            Integer sessionId = request.getSessionId();

            log.info("초기 intentType = {}", intentType);


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
                    log.info("GPT 의도 분류 결과: {}", intentText);

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

            String prompt;
            switch (intentType) {
                case RECOMMEND_PROFILE:
                    // 1. 유저 성향 요약
                    String summary = userProfileService.buildProfileSummaryByUserId(userId);
                    String riskType = userProfileService.getRiskTypeByUserId(userId);


                    // 2. 종목 리스트 가져오기 (거래량 상위 등)
                    List<Map<String, Object>> rawStocks = volumeRankingApi.getCombinedVolumeRanking(3, "0");


                    // 3. 성향 기반 필터링
                    List<RecommendationStock> recStocks = rawStocks.stream()
                            .map(ProfileStockMapper::fromMap)
                            .toList();

                    List<RecommendationStock> filteredStocks = ProfileStockFilter.filterByRiskType(riskType, recStocks);

                    // 4. 종목 코드/이름 추출
                    List<String> tickers = filteredStocks.stream().map(RecommendationStock::getCode).toList();
                    List<String> names = filteredStocks.stream().map(RecommendationStock::getName).toList();


                    // 5. 상세 정보 조회 (PriceApi 이용)
                    List<RecommendationStock> detailed = profileStockRecommender.getRecommendedStocksByProfile(tickers, names);

                    // 6. DTO로 매핑 (ChatAnalysisDto)
                    List<ChatAnalysisDto> analysisList = detailed.stream()
                            .map(ChatAnalysisMapper::toDto)
                            .toList();

                    // 7. DB 저장 (선택사항)
                    for (ChatAnalysisDto dto : analysisList) {
                        chatBotMapper.insertAnalysis(dto); // 직접 만든 insertAnalysis() 메서드
                    }

                    // 8. GPT 프롬프트 구성
                    prompt = promptBuilder.buildForProfile(userId, summary, analysisList);

                    break;


                case RECOMMEND_KEYWORD:
                    prompt = promptBuilder.buildForKeyword(userMessage);
                    break;

                case STOCK_ANALYZE:
                    prompt = promptBuilder.buildForAnalysis(userMessage);
                    break;

                case PORTFOLIO_ANALYZE:
                    prompt = promptBuilder.buildForPortfolioAnalysis(userId);
                    break;

                case SESSION_END:
                    prompt = "대화를 종료합니다. 감사합니다.";
                    break;

                case ERROR:
                    prompt = "사용자 입력에 오류가 있습니다. 다시 확인해주세요.";
                    break;

                case UNKNOWN:
                    prompt = "요청 내용을 이해하지 못했습니다. 다시 질문해주세요.";
                    break;

                case MESSAGE:
                default:
                    prompt = userMessage;
                    log.info("🧠 GPT에 보낼 프롬프트:\n{}", prompt);
                    break;
            }
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

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
            // TODO: 종목코드 추출 API 연동 필요 -> 추천 데이터 저장

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
                User: "내 투자 성향으로 추천해줘"
                Answer: RECOMMEND_PROFILE
                
                Example 3:
                User: "내 성향에 맞는 주식 뭐야?"
                Answer: RECOMMEND_PROFILE
                
                Example 4:
                User: "성향 기반으로 추천해줘"
                Answer: RECOMMEND_PROFILE
                
                Example 5:
                User: "삼성전자 분석해줘"
                Answer: STOCK_ANALYZE

    User: %s
    """.formatted(userMessage);
    }

}
