package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.util.chatbot.OpenAiClient;
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


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    private final PromptBuilder promptBuilder;

    @Autowired
    private OpenAiClient openAiClient;

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


            if (intentType == null || intentType == IntentType.MESSAGE) {
                String prompt = buildIntentClassificationPrompt(userMessage);

                // GPT 호출
                String intentText = openAiClient.getChatCompletion(prompt);

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
                log.info("intend = null 세션 새로 생성... sessionId = {}", sessionId);
                ChatSessionDto newSession = ChatSessionDto.builder()
                        .userId(userId)
                        .lastIntent(intentType)
                        .build();
                chatBotMapper.insertChatSession(newSession);
                sessionId = newSession.getId();
            } else {
                // 기존 세션의 마지막 intent 가져옴
                log.info("기존 세션 intent 확인: sessionId = {}, userId = {}", sessionId, userId);
                IntentType lastIntent = chatBotMapper.getLastIntentBySessionId(sessionId);
                log.info("기존 세션의 lastIntent = {}, 현재 요청 intent = {}", lastIntent, intentType);

                if (!intentType.equals(lastIntent)) {
                    // intent 바뀜 → 이전 세션 종료 + 새 세션 생성
                    log.info("🔄 intent가 변경됨 → 세션 종료 + 새 세션 생성");

                    chatBotMapper.endChatSession(sessionId);
                    log.info("☑ 종료된 세션 ID = {}, 종료 결과 = {}", sessionId);

                    ChatSessionDto newSession = ChatSessionDto.builder()
                            .userId(userId)
                            .lastIntent(intentType)
                            .build();
                    chatBotMapper.insertChatSession(newSession);
                    sessionId = newSession.getId();
                    log.info("🆕 새 세션 생성됨: sessionId = {}, intent = {}", sessionId, intentType);
                } else {
                    log.info("♻️ intent 동일 → lastIntent만 갱신");
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
                    log.info("summary: {}", summary);
                    log.info("riskType: {}", riskType);



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

                    // 7. DB 저장 (추천된 종목의 데이터를 저장)
                    for (ChatAnalysisDto dto : analysisList) {
                        chatBotMapper.insertAnalysis(dto); // 직접 만든 insertAnalysis() 메서드
                    }

                    // 8-1. GPT 분석 요청 프롬프트에 요청 ( 응답 json)
                    String analysisPrompt = promptBuilder.buildForStockInsights(analysisList);
                    String analysisResponse = openAiClient.getChatCompletion(analysisPrompt);

                    // 8-3. 추천 사유 파싱 → DB 저장

                    List<ChatRecommendationDto> recResults = parseRecommendationText(analysisResponse, analysisList, userId, riskType);
                    for (ChatRecommendationDto dto : recResults) {
                        chatBotMapper.insertRecommendation(dto);
                    }


                    // 8. GPT 프롬프트 구성
                    prompt = promptBuilder.buildSummaryFromRecommendations(summary, recResults,analysisList);

                    log.info("🧠 GPT에 보낼 프롬프트:\n{}", prompt);


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
            String content = openAiClient.getChatCompletion(prompt);

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

        try {
            if (intentType != null && intentType != IntentType.ERROR) {
                Integer activeSessionId = chatBotMapper.getActiveSessionIdByUserId(userId);
                if (activeSessionId != null) {
                    chatBotMapper.endChatSession(activeSessionId);
                    log.info("❌ 에러 발생으로 세션 종료: sessionId = {}", activeSessionId);
                }
            }
        } catch (Exception sessionEx) {
            log.warn("세션 종료 중 예외 발생: {}", sessionEx.getMessage());
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

    // 파싱 메서드
    public List<ChatRecommendationDto> parseRecommendationText(
            String gptResponse, List<ChatAnalysisDto> stockList, Integer userId, String riskType) {

        List<ChatRecommendationDto> result = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(gptResponse);

            for (JsonNode node : root) {
                String ticker = node.get("ticker").asText();
                String reason = node.get("reason").asText();

                ChatAnalysisDto stock = stockList.stream()
                        .filter(s -> s.getTicker().equals(ticker))
                        .findFirst()
                        .orElse(null);

                if (stock == null) continue;

                result.add(ChatRecommendationDto.builder()
                        .userId(userId)
                        .ticker(ticker)
                        .recommendType("RECOMMEND_PROFILE")
                        .reason(reason)
                        .riskLevel(null)
                        .expectedReturn(null)
                        .riskType(riskType)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

        } catch (Exception e) {
            log.warn("❗ GPT 응답 파싱 실패: {}", e.getMessage());
        }

        return result;
    }


}
