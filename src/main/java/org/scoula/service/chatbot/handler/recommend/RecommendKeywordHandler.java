package org.scoula.service.chatbot.handler.recommend;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.ChatAnalysisDto;
import org.scoula.domain.chatbot.dto.ChatRecommendationDto;
import org.scoula.domain.chatbot.dto.RecommendationStock;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.scoula.service.chatbot.ProfileStockRecommender;
import org.scoula.service.chatbot.PromptBuilder;
import org.scoula.service.chatbot.UserProfileService;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.scoula.util.chatbot.ChatAnalysisMapper;
import org.scoula.util.chatbot.OpenAiClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;


/**
 * RecommendKeywordHandler
 *
 * <p>IntentType.RECOMMEND_KEYWORD 에 해당하는 핸들러.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>사용자가 입력한 메시지에서 키워드를 추출한다.</li>
 *   <li>GPT 기반으로 키워드와 관련된 종목 후보를 받아온다.</li>
 *   <li>후보 종목을 모의투자/거래 데이터와 결합하여 세부 지표를 조회한다.</li>
 *   <li>PER, PBR, ROE 등 기본 필터링을 적용해 유효한 종목을 선별한다.</li>
 *   <li>최종 후보를 GPT에 다시 전달해 추천 사유를 생성한다.</li>
 *   <li>추천/분석 결과를 DB에 저장하고 사용자에게 전달한다.</li>
 * </ul>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>키워드 → 종목 후보 → 상세 지표 → GPT 분석 → 추천 저장 흐름</li>
 *   <li>fallback 로직: 필터링 결과가 비어 있으면 기본 조건(positive check)으로 5개 선별</li>
 *   <li>@Transactional: 추천/분석 저장을 하나의 트랜잭션으로 처리</li>
 * </ul>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class RecommendKeywordHandler implements IntentHandler {

    private final UserProfileService userProfileService;
    private final ProfileStockRecommender profileStockRecommender;
    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;
    private final ChatBotMapper chatBotMapper;

    @Override public IntentType supports() { return IntentType.RECOMMEND_KEYWORD; }

    /**
     * 키워드 기반 추천 처리
     * 1) 키워드 추출
     * 2) 종목 후보 수집 (GPT)
     * 3) 상세 지표 조회 (profileStockRecommender)
     * 4) 기본 필터링 적용
     * 5) GPT 분석 & 추천 사유 생성
     * 6) 분석/추천 결과 DB 저장
     * 7) 최종 메시지 반환
     */
    @Override
    @Transactional
    public ExecutionResult handle(ExecutionContext ctx) throws Exception {
        Integer userId = ctx.getUserId();
        String riskType = userProfileService.getRiskTypeByUserId(userId);

        // 1) 메시지에서 키워드 추출 (GPT)
        String keyword = extractKeywordFromMessage(ctx.getUserMessage());
        // 2) 키워드 기반 종목 후보 수집 (GPT)
        var seeds = getStocksByKeyword(keyword);
        // 3) 상세 지표 조회 (모의투자 API/DB 활용)
        var detailed = getDetailedStocks(seeds);
        // 4) 기본 필터링
        var valid = filterByDefault(detailed);
        // fallback: 필터링 결과 없을 시 Positive 조건으로 top 5 선별
        if (valid.isEmpty()) {
            valid = detailed.stream()
                    .filter(s -> positive(s.getPer()) && positive(s.getPbr())
                            && positive(s.getRoe()) && positive(s.getEps())
                            && positive(s.getVolume()) && positive(s.getPrice()))
                    .sorted(Comparator.comparingDouble(RecommendationStock::getVolume).reversed())
                    .limit(5)
                    .toList();
        }

        // 5) 분석 결과 저장 (chat_analysis)
        var analysisList = valid.stream().map(ChatAnalysisMapper::toDto).toList();
        analysisList.forEach(chatBotMapper::insertAnalysis);

        // 6) GPT 호출로 추천 사유 생성
        String prompt = promptBuilder.buildForStockInsights(analysisList);
        String gptJson = openAiClient.getChatCompletion(prompt);

        // 7) 추천 결과 파싱 후 DB 저장 (chat_recommendations)
        parseAndSaveRecommendations(gptJson, analysisList, userId, riskType);

        // 8) 사용자 응답 반환
        String finalText = "🎯 키워드 기반 추천드릴게요!\n\n" + gptJson;
        return ExecutionResult.builder().finalContent(finalText).build();
    }

    /* ====== 내부 보조 ====== */
    /** GPT를 이용해 사용자 메시지에서 키워드 추출 */
    private String extractKeywordFromMessage(String userMessage) {
        try {
            String prompt = promptBuilder.buildKeywordExtractionPrompt(userMessage);
            String gptResponse = openAiClient.getChatCompletion(prompt);
            var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(gptResponse);
            if (root.has("keyword")) return root.get("keyword").asText();
        } catch (Exception e) {
            log.warn("키워드 추출 실패: {}", e.getMessage());
        }
        return userMessage;
    }
    /** GPT를 이용해 키워드에 해당하는 종목 후보 수집 */
    private List<RecommendationStock> getStocksByKeyword(String keyword) {
        try {
            String prompt = promptBuilder.buildForKeyword(keyword);
            String response = openAiClient.getChatCompletion(prompt);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(response, mapper.getTypeFactory()
                    .constructCollectionType(List.class, RecommendationStock.class));
        } catch (Exception e) {
            log.warn("GPT 종목+티커 추천 실패: {}", e.getMessage());
            return List.of();
        }
    }
    /** 모의투자/DB 기반으로 종목 상세 지표 조회 */
    private List<RecommendationStock> getDetailedStocks(List<RecommendationStock> stocks) {
        var tickers = stocks.stream().map(RecommendationStock::getCode).toList();
        var names = stocks.stream().map(RecommendationStock::getName).toList();
        return profileStockRecommender.getRecommendedStocksByProfile(tickers, names);
    }
    /** Double 값이 유효하고 양수인지 검사 */
    private static boolean positive(Double v) {
        return v != null && !v.isNaN() && !v.isInfinite() && v > 0.0;
    }

    /** Double 값이 주어진 범위 안에 있는지 검사 */
    private static boolean inRange(Double v, double minIncl, double maxIncl) {
        return v != null && !v.isNaN() && !v.isInfinite() && v >= minIncl && v <= maxIncl;
    }

    /** 기본 필터링: PER/PBR/ROE/EPS/거래량/가격 등 조건으로 선별 */
    private static List<RecommendationStock> filterByDefault(List<RecommendationStock> list) {
        return list.stream()
                .filter(s -> positive(s.getPer()))
                .filter(s -> positive(s.getPbr()))
                .filter(s -> positive(s.getRoe()))
                .filter(s -> positive(s.getEps()))          // ← EPS 0/음수/NaN/Inf 제외
                .filter(s -> positive(s.getVolume()))
                .filter(s -> positive(s.getPrice()))
                .filter(s -> inRange(s.getPer(), 0.0, 80.0))
                .filter(s -> inRange(s.getPbr(), 0.0, 20.0))
                .filter(s -> inRange(s.getRoe(), 0.0, 60.0))
                .toList();
    }

    /** GPT JSON 추천 결과 파싱 후 DB 저장 */
    private void parseAndSaveRecommendations(String gptJson, List<ChatAnalysisDto> stockList,
                                             Integer userId, String riskType) {
        try {
            var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(gptJson);
            for (var node : root) {
                var ticker = node.get("ticker").asText();
                var reason = node.get("reason").asText();
                boolean exists = stockList.stream().anyMatch(s -> s.getTicker().equals(ticker));
                if (!exists) continue;

                var dto = ChatRecommendationDto.builder()
                        .userId(userId)
                        .ticker(ticker)
                        .recommendType(IntentType.RECOMMEND_KEYWORD.name())
                        .reason(reason)
                        .riskType(riskType)
                        .createdAt(LocalDateTime.now())
                        .build();
                chatBotMapper.insertRecommendation(dto);
            }
        } catch (Exception e) {
            log.warn("추천 파싱 실패: {}", e.getMessage());
        }
    }
}