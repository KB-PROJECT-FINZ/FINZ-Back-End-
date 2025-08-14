package org.scoula.service.chatbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.service.chatbot.intent.IntentResolver;
import org.scoula.service.chatbot.message.MessageService;
import org.scoula.service.chatbot.session.ChatSessionService;
import org.scoula.service.trading.TradingService;
import org.scoula.util.chatbot.*;
import org.scoula.api.mocktrading.VolumeRankingApi;
import org.scoula.domain.chatbot.dto.*;
import org.scoula.domain.chatbot.enums.ErrorType;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;
    private final ProfileStockRecommender profileStockRecommender;
    private final VolumeRankingApi volumeRankingApi;
    private final UserProfileService userProfileService;
    private final StockNameParser stockNameParser;
    private final ChatBotMapper chatBotMapper;
    private final ObjectMapper objectMapper;
    private final TradingService tradingService;
    private final IntentResolver intentResolver;
    private final MessageService messageService;

    // 세션
    private final ChatSessionService chatSessionService;

    //피드백 기간 설정
    public int extractPeriodDays(String message) {
        if (message.contains("6개월")) return 180;
        if (message.contains("3개월")) return 90;
        if (message.contains("1개월")) return 30;
        return 30; // 기본값
    }

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

            // ========================2. 전처리======================
            // TODO: 민감 정보 마스킹 로직

            // 세션 관리 (intent 바뀌면 종료하고 새 세션 생성)
            sessionId = chatSessionService.ensureSession(userId, sessionId, intentType);

            // ====================== 4. 사용자 메시지 저장 ======================
            // chat_messages 테이블에 사용자 메시지 저장
            messageService.save(userId, sessionId, "user", userMessage, intentType);
            log.info("[MESSAGE] 사용자 메시지 저장 완료");

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

            BehaviorStatsDto stats = null;
            String prompt = null;
            String content = null;
            String gptAnalysisResponse = null;
            Integer requestedPeriod = null;
            ChatMessageDto gptMessage = null;
            boolean assistantSaved = false;


            switch (intentType) {

                case RECOMMEND_PROFILE: {
                    // 1. 사용자 투자 성향 요약 및 위험 성향 조회
                    String summary = userProfileService.buildProfileSummaryByUserId(userId);
                    String riskType = userProfileService.getRiskTypeByUserId(userId);

                    // 2. 거래량 기준 상위 종목 조회 (중복 제거 포함)
                    List<RecommendationStock> topVolumeStocks = getTopVolumeStocks(50);

                    // 3. 종목 상세 정보 조회 (가격, 지표 등)
                    List<RecommendationStock> detailedStocks = getDetailedStocks(topVolumeStocks);


                    // 유효성 필터 적용 (0/음수/null 제거)
                    List<RecommendationStock> cleanedStocks = filterByDefault(detailedStocks);
                    if (cleanedStocks.isEmpty()) {
                        log.warn("유효성 필터 통과 0개 → fallback");
                        cleanedStocks = topVolumeStocks.stream().limit(3).toList();
                    }

                    // 4. 성향 필터는 유효값만 대상으로
                    List<RecommendationStock> filteredStocks = filterStocksByRiskType(riskType, cleanedStocks);



                    // 5. 필터링된 종목들을 분석용 DTO로 변환하고 DB 저장
                    List<ChatAnalysisDto> analysisList = convertToAnalysisDtos(filteredStocks);
                    saveAnalysisListToDb(analysisList);

                    // 6. GPT에 분석 프롬프트 요청 후 JSON 응답 수신
                    gptAnalysisResponse = callAnalysisPrompt(analysisList);

                    // 7. GPT 응답(JSON)을 파싱하여 추천 사유 리스트 생성 및 DB 저장
                    List<ChatRecommendationDto> recResults = parseRecommendationText(gptAnalysisResponse, analysisList, userId, riskType,intentType);
                    saveRecommendationsToDb(recResults);

                    // 8. 투자 성향 기반 추천 안내 메시지 추가
                    gptAnalysisResponse = "🧠 투자 성향 기반 추천드릴게요!\n\n" + gptAnalysisResponse;

                    // 9. GPT 응답 저장
                    gptMessage = saveChatMessage(userId, sessionId, "assistant", gptAnalysisResponse, intentType);

                    break;
                }

                case RECOMMEND_KEYWORD: {
                    String riskType = userProfileService.getRiskTypeByUserId(userId);
                    // 1. 사용자 키워드 입력받기 (추출)
                    String keyword = extractKeywordFromMessage(userMessage);
                    log.info(keyword);
                    // 2. 키워드를 기반으로 GPT에게 관련 종목 20개 추천 요청 (종목명만 추출되게 프롬프트로 강제)
                    List<RecommendationStock> stockList = getStocksByKeyword(keyword);
                    log.info("📥 GPT 추천 종목 수: {}", stockList.size());

                    // 3. 종목 리스트에 대해 상세조회 API 호출 → 기존 상세조회 로직 재사용
                    List<RecommendationStock> detailed = getDetailedStocks(stockList);

                    // 4. 상세 데이터 기반으로 필터링 (투자 지표 등 기준으로 추림)
                    List<RecommendationStock> filtered = filterByDefault(detailed);
                    log.info("🧪 필터링된 종목 수: {}, 리스트: {}", filtered.size(), filtered);

                    // 5. 필터링된 종목들을 GPT 분석 프롬프트에 넣어서 분석 요청 → 기존 분석 프롬프트 재사용
                    List<ChatAnalysisDto> analysisList = convertToAnalysisDtos(filtered);
                    saveAnalysisListToDb(analysisList);
                    log.info("📊 분석용 DTO 변환 완료, 개수: {}, 리스트: {}", analysisList.size(), analysisList);

                    // 6. 분석 결과 기반으로 요약 프롬프트 구성 → 기존 응답 프롬프트 재사용
                    gptAnalysisResponse = callAnalysisPrompt(analysisList);
                    log.info("🧠 GPT 분석 응답: {}", gptAnalysisResponse);
                    
                    // 7. 키워드 기반 추천 안내 메시지 추가
                    gptAnalysisResponse = "🎯 키워드 기반 추천드릴게요!\n\n" + gptAnalysisResponse;
                    
                    // 8. GPT 응답 저장
                    gptMessage = saveChatMessage(userId, sessionId, "assistant", gptAnalysisResponse, intentType);

                    break;
                }
                case STOCK_ANALYZE:
                    // 1. 종목명 추출 프롬프트 생성 및 GPT 호출
                    prompt = promptBuilder.stockextractionPrompt(userMessage);
                    String gptResponse = openAiClient.getChatCompletion(prompt);

                    // 2. GPT 응답 파싱 → 종목명, 티커 추출
                    StockExtractionResultDto result = stockNameParser.parseStockExtraction(gptResponse);

                    // 3. 종목명 누락 시 예외 처리
                    if (result.getStockName() == null || result.getStockName().isBlank()) {
                        gptAnalysisResponse = "❌ 종목명을 정확히 입력해주세요.";
                        break;
                    }

                    // 4. 종목명 + 티커로 RecommendationStock 객체 생성
                    RecommendationStock raw = RecommendationStock.builder()
                            .name(result.getStockName())
                            .code(result.getTicker())
                            .build();

                    // 5. 상세 정보 API 조회 (PER, PBR 등)
                    List<RecommendationStock> detailed = getDetailedStocks(List.of(raw));
                    if (detailed.isEmpty()) {
                        gptAnalysisResponse = "❌ 해당 종목의 상세 정보를 찾을 수 없습니다.";
                        break;
                    }

                    // 6. 분석용 DTO 변환
                    ChatAnalysisDto dto = ChatAnalysisMapper.toDto(detailed.get(0));

                    // 7. GPT 분석 프롬프트 호출 (종목 분석 전용 프롬프트 사용)
                    String stockAnalysisPrompt = promptBuilder.buildForStockAnalysis(List.of(dto));
                    gptAnalysisResponse = openAiClient.getChatCompletion(stockAnalysisPrompt);
                    
                    // 8. 종목 분석 결과 안내 메시지 추가
                    gptAnalysisResponse = "🔍 종목 분석 결과입니다.\n\n" + gptAnalysisResponse;
                    
                    // 9. GPT 응답 저장
                    gptMessage = saveChatMessage(userId, sessionId, "assistant", gptAnalysisResponse, intentType);

                    break;

                case PORTFOLIO_ANALYZE: {
                    log.info("[GPT] 포트폴리오 분석 프롬프트 생성 시작");

                    // 1. 사용자 요청 분석 기간 추출
                    requestedPeriod = extractPeriodDays(userMessage);
                    final int finalRequestedPeriod = requestedPeriod;
                    log.info("📆 사용자 요청 분석 기간: {}일", requestedPeriod);

                    // 2. 거래 요약 정보 조회
                    stats = tradingService.getBehaviorStats(userId, requestedPeriod);

                    if (stats == null) {
                        return ChatResponseDto.builder()
                                .content("📊 선택한 기간 동안 거래 내역이 없습니다.")
                                .intentType(intentType)
                                .sessionId(sessionId)
                                .analysisPeriod(null)
                                .requestedPeriod(requestedPeriod)
                                .build();
                    }

                    log.info("[📊 Stats] 거래 요약 정보 - 시작일: {}, 종료일: {}", stats.getAnalysisStart(), stats.getAnalysisEnd());

                    // 3. GPT 요청 프롬프트 구성
                    prompt = promptBuilder.buildForPortfolioAnalysis(stats);

                    // 4. GPT 호출
                    content = openAiClient.getChatCompletion(prompt);
                    log.warn("GPT 응답 원문 ↓↓↓↓↓↓↓↓↓↓↓\n{}", content);

                    // 5. GPT 응답 저장
                    gptMessage = messageService.save(userId, sessionId, "assistant", content, intentType);
                    assistantSaved = true;

                    // 6. GPT 응답 파싱
                    ChatBehaviorFeedbackDto parsed = extractSummaryParts(content);

                    if (parsed == null) {
                        return ChatResponseDto.builder()
                                .content("❌ 분석 결과를 파싱하는 중 문제가 발생했습니다. 형식을 확인해주세요.")
                                .intentType(intentType)
                                .sessionId(sessionId)
                                .requestedPeriod(requestedPeriod)
                                .build();
                    }

                    // 7. 피드백 저장
                    parsed.setUserId(userId);
                    parsed.setSessionId(sessionId);
                    parsed.setMessageId(gptMessage.getId());
                    parsed.setTransactionCount(stats.getTransactionCount());
                    chatBotMapper.insertChatBehaviorFeedback(parsed);

                    // 8. 연관 거래 내역 저장
                    List<TransactionDTO> transactions = tradingService.getUserTransactions(userId);
                    transactions.sort(Comparator.comparing(TransactionDTO::getExecutedAt));
                    final BehaviorStatsDto finalStats = stats;

                    List<Long> transactionIds = transactions.stream()
                            .filter(tx -> {
                                LocalDate txDate = tx.getExecutedAt().toLocalDate();
                                return !txDate.isBefore(finalStats.getAnalysisStart()) && !txDate.isAfter(finalStats.getAnalysisEnd());
                            })
                            .map(tx -> (long) tx.getTransactionId())
                            .collect(Collectors.toList());

                    for (Long txId : transactionIds) {
                        chatBotMapper.insertChatBehaviorFeedbackTransaction(parsed.getId(), txId);
                    }

                    break;
                }
                
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
                    log.info("[GPT] 기본 대화 프롬프트 사용 → {}", prompt);
                    break;
            }

            // ====================== 8. GPT 응답 저장 ======================
            // chat_messages 테이블에 GPT 응답 저장

            String finalResponse = (gptAnalysisResponse != null && !gptAnalysisResponse.isBlank()) ? gptAnalysisResponse : openAiClient.getChatCompletion(prompt);
          

            // content가 비어있고 prompt만 있는 케이스 보완
            if (finalResponse == null || finalResponse.isBlank()) {
                finalResponse = openAiClient.getChatCompletion(prompt);
            }

            // 메세지 이미 저장했으면 또 저장하지 않음
            if (!assistantSaved) {
                gptMessage = messageService.save(userId, sessionId, "assistant", finalResponse, intentType);
            }

            // gptMessage가 아직 저장되지 않은 경우에만 저장
            if (gptMessage == null) {
                gptMessage = saveChatMessage(userId, sessionId, "assistant", finalResponse, intentType);
            }

            // ====================== 9. 최종 응답 반환 ======================
            return ChatResponseDto.builder()
                    .content(finalResponse.trim())
                    .intentType(intentType)
                    .messageId(gptMessage != null ? gptMessage.getId() : null)
                    .sessionId(sessionId)
                    .requestedPeriod(requestedPeriod)
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

    // 메세지 저장 함수
    private ChatMessageDto saveChatMessage(Integer userId, Integer sessionId, String role, String content, IntentType intentType) {
        ChatMessageDto message = ChatMessageDto.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .intentType(intentType)
                .build();
        
        return messageService.save(userId, sessionId, role, content, intentType);
    }

    public List<RecommendationStock> getStocksByKeyword(String keyword) {
        try {
            String prompt = promptBuilder.buildForKeyword(keyword);
            String response = openAiClient.getChatCompletion(prompt);
            log.info("🧠실제GPT 요청 프롬프트 ↓↓↓↓↓\n{}", prompt);

            log.info("🧠 GPT 종목 + 티커 응답: {}", response);

            return objectMapper.readValue(response, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("⚠ GPT 종목+티커 추천 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // 키워드 추출 함수
    private String extractKeywordFromMessage(String userMessage) {
        try {
            String prompt = promptBuilder.buildKeywordExtractionPrompt(userMessage);
            String gptResponse = openAiClient.getChatCompletion(prompt);

            JsonNode root = objectMapper.readTree(gptResponse);
            if (root.has("keyword")) {
                String keyword = root.get("keyword").asText();
                log.info("GPT로부터 추출된 키워드: {}", keyword);
                return keyword;
            }
        } catch (Exception e) {
            log.warn("⚠ 키워드 추출 실패, 사용자 원문 사용 → {}", e.getMessage());
        }
        return userMessage; // 실패하면 원문 그대로 사용
    }

    // GPT 응답(JSON)에서 추천 사유 파싱하여 DTO 리스트로 변환
    public List<ChatRecommendationDto> parseRecommendationText(
            String gptResponse, List<ChatAnalysisDto> stockList, Integer userId, String riskType,IntentType intentType) {

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
                        .recommendType(intentType.name())
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

    // 랭킹API -> 거개 상위 종목 조회
    private List<RecommendationStock> getTopVolumeStocks(int count) throws IOException {
        List<Map<String, Object>> rawStocks = volumeRankingApi.getCombinedVolumeRanking(count, "0");
        log.info("[GPT] 거래량 상위 종목 수신 완료 → {}개", rawStocks.size());
        return rawStocks.stream()
                .map(ProfileStockMapper::fromMap)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                RecommendationStock::getCode,
                                s -> s,
                                (s1, s2) -> s1 // 중복 제거
                        ),
                        map -> new ArrayList<>(map.values())
                ));
    }

    // 상세 조회API -> 상세 조회
    private List<RecommendationStock> getDetailedStocks(List<RecommendationStock> stocks) {
        List<String> tickers = stocks.stream().map(RecommendationStock::getCode).toList();
        List<String> names = stocks.stream().map(RecommendationStock::getName).toList();
        List<RecommendationStock> detailed = profileStockRecommender.getRecommendedStocksByProfile(tickers, names);
        log.info("[GPT] 상세 정보 조회 완료 → {}개", detailed.size());
        return detailed;
    }

    // 리스크 타입 필터링
    private List<RecommendationStock> filterStocksByRiskType(String riskType, List<RecommendationStock> stocks) {
        List<RecommendationStock> filtered = ProfileStockFilter.filterByRiskType(riskType, stocks);
        if (filtered.isEmpty()) {
            log.warn("⚠️ [{}] 조건 통과 종목 없음 → fallback 사용", riskType);
            return stocks.subList(0, Math.min(3, stocks.size()));
        }
        log.info("[GPT] 성향 기반 필터링 완료 → {}개", filtered.size());
        return filtered;
    }

    // 분석내용을 저장하기위한 Dto로 변환
    private List<ChatAnalysisDto> convertToAnalysisDtos(List<RecommendationStock> stocks) {
        return stocks.stream()
                .map(ChatAnalysisMapper::toDto)
                .toList();
    }

    // 실제 저장 분석기록 dto -> db
    private void saveAnalysisListToDb(List<ChatAnalysisDto> analysisList) {
        for (ChatAnalysisDto dto : analysisList) {
            chatBotMapper.insertAnalysis(dto);
        }
    }

    // 분석 내용 프롬프트 호출
    private String callAnalysisPrompt(List<ChatAnalysisDto> analysisList) {
        String prompt = promptBuilder.buildForStockInsights(analysisList);
        log.info("[GPT] 분석용 프롬프트 내용 ↓↓↓↓↓↓↓\n{}", prompt);
        return openAiClient.getChatCompletion(prompt);
    }

    // 추천사유 분석GPT 응답으로 부터 파싱 -> 저장
    private void saveRecommendationsToDb(List<ChatRecommendationDto> recommendations) {
        for (ChatRecommendationDto dto : recommendations) {
            chatBotMapper.insertRecommendation(dto);
        }
        log.info("[GPT] GPT 응답 기반 추천 사유 파싱 완료 → {}개", recommendations.size());
    }

    // 필터링 함수
    public static List<RecommendationStock> filterByDefault(List<RecommendationStock> stocks) {
        return stocks.stream()
                .filter(stock ->
                        isValid(stock.getPer()) &&
                                isValid(stock.getPbr()) &&
                                isValid(stock.getRoe()) &&
                                isValid(stock.getVolume()) &&
                                isValid(stock.getPrice())
                )
                .collect(Collectors.toList());
    }

    private static boolean isValid(Double value) {
        return value != null && value > 0;
    }

    //피드백 파싱
    private ChatBehaviorFeedbackDto extractSummaryParts(String content) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // LocalDate 지원 추가
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 문자열 형식 유지

            return objectMapper.readValue(content, ChatBehaviorFeedbackDto.class);
        } catch (Exception e) {
            log.error("❌ GPT 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

}

