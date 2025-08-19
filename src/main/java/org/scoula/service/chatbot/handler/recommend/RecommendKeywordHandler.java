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

    @Override
    @Transactional
    public ExecutionResult handle(ExecutionContext ctx) throws Exception {
        Integer userId = ctx.getUserId();
        String riskType = userProfileService.getRiskTypeByUserId(userId);

        String keyword = extractKeywordFromMessage(ctx.getUserMessage());
        var seeds = getStocksByKeyword(keyword);

        var detailed = getDetailedStocks(seeds);
        var valid = filterByDefault(detailed);

        if (valid.isEmpty()) {
            valid = detailed.stream()
                    .filter(s -> positive(s.getPer()) && positive(s.getPbr())
                            && positive(s.getRoe()) && positive(s.getEps())
                            && positive(s.getVolume()) && positive(s.getPrice()))
                    .sorted(Comparator.comparingDouble(RecommendationStock::getVolume).reversed())
                    .limit(5)
                    .toList();
        }

        var analysisList = valid.stream().map(ChatAnalysisMapper::toDto).toList();
        analysisList.forEach(chatBotMapper::insertAnalysis);

        String prompt = promptBuilder.buildForStockInsights(analysisList);
        String gptJson = openAiClient.getChatCompletion(prompt);

        parseAndSaveRecommendations(gptJson, analysisList, userId, riskType);

        String finalText = "🎯 키워드 기반 추천드릴게요!\n\n" + gptJson;
        return ExecutionResult.builder().finalContent(finalText).build();
    }

    /* ====== 내부 보조 ====== */
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
    private List<RecommendationStock> getDetailedStocks(List<RecommendationStock> stocks) {
        var tickers = stocks.stream().map(RecommendationStock::getCode).toList();
        var names = stocks.stream().map(RecommendationStock::getName).toList();
        return profileStockRecommender.getRecommendedStocksByProfile(tickers, names);
    }

    private static boolean positive(Double v) {
        return v != null && !v.isNaN() && !v.isInfinite() && v > 0.0;
    }

    private static boolean inRange(Double v, double minIncl, double maxIncl) {
        return v != null && !v.isNaN() && !v.isInfinite() && v >= minIncl && v <= maxIncl;
    }

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