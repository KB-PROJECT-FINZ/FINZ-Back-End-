package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.service.trading.TradingService;
import org.scoula.util.chatbot.*;
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

    private final TradingService tradingService; // ✅ 이 줄 추가


    @Override
    public ChatResponseDto getChatResponse(ChatRequestDto request) {
        try {
            // ====================== 1. 입력 데이터 추출 ======================
            String userMessage = request.getMessage();
            IntentType intentType = request.getIntentType();
            Integer userId = request.getUserId();
            Integer sessionId = request.getSessionId();

            log.info("초기 intentType = {}", intentType);


            // 프론트에서 명시한 intentType이 MESSAGE거나 null인 경우만 GPT 분류
            if (intentType == null || intentType == IntentType.MESSAGE) {
                log.info("🧠 GPT 분류 수행 시작...");
                String prompt = buildIntentClassificationPrompt(userMessage);

                // GPT 호출
                String intentText = openAiClient.getChatCompletion(prompt);

                try {
                    intentType = IntentType.valueOf(intentText);
                    log.info("🧠 GPT 의도 분류 결과: {}", intentText);
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
                log.info("[SESSION] 새 세션 생성 완료 → sessionId: {}, intentType: {}", sessionId, intentType);
            } else {
                // 기존 세션의 마지막 intent 가져옴
                log.info("기존 세션의 마지막 intent 가져옴... sessionId = {}", sessionId);
                IntentType lastIntent = chatBotMapper.getLastIntentBySessionId(sessionId);

                if (!intentType.equals(lastIntent)) {
                    // intent 바뀜 → 이전 세션 종료 + 새 세션 생성
                    log.info("세션 종료 시도: {}", sessionId);
                    chatBotMapper.endChatSession(sessionId);
                    log.info("[SESSION] ☑ 기존 세션 종료 완료 → sessionId: {}", sessionId);

                    ChatSessionDto newSession = ChatSessionDto.builder()
                            .userId(userId)
                            .lastIntent(intentType)
                            .build();
                    chatBotMapper.insertChatSession(newSession);
                    sessionId = newSession.getId();
                    log.info("[SESSION] 🆕 새 세션 생성 완료 → sessionId: {}, intentType: {}", sessionId, intentType);
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
            String content = "";
            BehaviorStatsDto stats = null;

            String prompt;
            switch (intentType) {

                case RECOMMEND_PROFILE:
                    // 1. 유저 성향 요약
                    String summary = userProfileService.buildProfileSummaryByUserId(userId);
                    String riskType = userProfileService.getRiskTypeByUserId(userId);
                    log.info("[GPT] 사용자 성향 summary: {}", summary);
                    log.info("[GPT] 사용자 riskType: {}", riskType);


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

                    // 8-1. GPT 분석 요청 프롬프트
                    String analysisPrompt = promptBuilder.buildForStockInsights(analysisList);
                    String analysisResponse = openAiClient.getChatCompletion(analysisPrompt);


                    log.info("[GPT] GPT 분석 요청 프롬프트 구성 완료");
                    log.info("📝 [GPT] 분석용 프롬프트 내용 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓\n{}\n↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑", analysisPrompt);


                    // 8-3. 추천 사유 파싱 → DB 저장

                    // 5,6 에서 저장된 추천 종목의 값을 gpt로 보내서 상세한 분석 요청
                    // 분석 후 이유와 상세한 기술적 지표, 설명 등 응답 하게 만듦.
                    // 추천한 이유를 DB에 저장(ChatRecommendationDto.reason)

                    // 8. GPT 프롬프트 구성
                    prompt = promptBuilder.buildForProfile(userId, summary, analysisList);

                    break;


                case RECOMMEND_KEYWORD:
                    prompt = promptBuilder.buildForKeyword(userMessage);
                    log.info("[GPT] 키워드 기반 추천 프롬프트 생성 완료");
                    break;

                case STOCK_ANALYZE:
                    prompt = promptBuilder.buildForAnalysis(userMessage);
                    log.info("[GPT] 종목 분석 프롬프트 생성 완료");
                    break;

                case PORTFOLIO_ANALYZE:
                    log.info("[GPT] 포트폴리오 분석 프롬프트 생성 완료");
                    // 1. 거래 요약 정보 조회
                    stats = tradingService.getBehaviorStats(userId);
                    if (stats == null) {
                        return ChatResponseDto.builder()
                                .content("📊 분석할 모의투자 내역이 없습니다.")
                                .intentType(intentType)
                                .sessionId(sessionId)
                                .build();
                    }
                    log.info("[📊 Stats] 거래 요약 정보: {}", stats);

                    // 2. 거래 요약 정보 기반 프롬프트 구성
                    prompt = promptBuilder.buildForPortfolioAnalysis(stats);

                    // 3. GPT 호출
                    content = openAiClient.getChatCompletion(prompt);

                    // 4. 메시지 저장
                    ChatMessageDto saved = saveChatMessage(userId, sessionId, "assistant", content, intentType);

                    // 5. 피드백 본문 요약
                    String[] parts = content.split("개선점\\s*:");
                    String feedbacksummary = parts[0].trim();
                    String suggestion = parts.length > 1 ? parts[1].trim() : null;

                    // 6. 리포트 저장
                    ChatBehaviorFeedbackDto feedback = ChatBehaviorFeedbackDto.builder()
                            .userId(userId)
                            .sessionId(sessionId)
                            .messageId(saved.getId())
                            .summaryText(feedbacksummary)
                            .suggestionText(suggestion)
                            .transactionCount(stats.getTransactionCount())
                            .analysisPeriod(stats.getAnalysisPeriod())
                            .startDate(stats.getStartDate())
                            .endDate(stats.getEndDate())
                            .build();
                    chatBotMapper.insertChatBehaviorFeedback(feedback);

                    // 7. 연관 거래내역 저장
                    List<Long> transactionIds = tradingService.getTransactionIdsByUser(userId);
                    for (Long txId : transactionIds) {
                        chatBotMapper.insertChatBehaviorFeedbackTransaction(feedback.getId(), txId);
                    }

                    break;


                case TERM_EXPLAIN:
                    prompt = promptBuilder.buildForTermExplain(userMessage);
                    break;

                case SESSION_END:
                    prompt = "대화를 종료합니다. 감사합니다.";
                    log.info("[GPT] 사용자 의도가 세션 종료");
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
            content = openAiClient.getChatCompletion(prompt);

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
    - TERM_EXPLAIN: Ask for explanation of a financial term (e.g., PER, ROE, EPS).
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
                
                Example 6:
                User: "ROE"
                Answer: TERM_EXPLAIN
                
                Example 7:
                User: "EPS가 뭔가요?"
                Answer: TERM_EXPLAIN

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
            log.warn("⚠️ [GPT] 추천 응답 파싱 실패: {}", e.getMessage());
            log.warn("⚠️ [GPT] 원시 응답 내용 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓");
            log.warn(gptResponse);
        }

        return result;
    }
}
