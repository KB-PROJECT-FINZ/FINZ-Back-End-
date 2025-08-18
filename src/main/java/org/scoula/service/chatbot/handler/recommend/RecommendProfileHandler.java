package org.scoula.service.chatbot.handler.recommend;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.api.mocktrading.VolumeRankingApi;
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
import org.scoula.util.chatbot.ProfileStockFilter;
import org.scoula.util.chatbot.ProfileStockMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class RecommendProfileHandler implements IntentHandler {

    private final UserProfileService userProfileService;
    private final VolumeRankingApi volumeRankingApi;
    private final ProfileStockRecommender profileStockRecommender;
    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;
    private final ChatBotMapper chatBotMapper;

    @Override public IntentType supports() { return IntentType.RECOMMEND_PROFILE; }

    @Override
    @Transactional
    public ExecutionResult handle(ExecutionContext ctx) throws Exception {
        Integer userId = ctx.getUserId();
        String riskType = userProfileService.getRiskTypeByUserId(userId);

        var top = getTopVolumeStocks(50);
        var detailed = getDetailedStocks(top);
        var valid = filterByDefault(detailed);

        if (valid.isEmpty()) {
            valid = detailed.stream()
                    .filter(s -> positive(s.getPer()) && positive(s.getPbr())
                            && positive(s.getRoe()) && positive(s.getEps())
                            && positive(s.getVolume()) && positive(s.getPrice()))
                    .sorted(Comparator.comparingDouble(RecommendationStock::getVolume).reversed())
                    .limit(50)
                    .toList();
        }

        var filtered = ProfileStockFilter.selectByRiskType(riskType, valid, 5);

        var analysisList = filtered.stream().map(ChatAnalysisMapper::toDto).toList();
        analysisList.forEach(chatBotMapper::insertAnalysis);

        String prompt = promptBuilder.buildForStockInsights(analysisList);
        String gptJson = openAiClient.getChatCompletion(prompt);

        parseAndSaveRecommendations(gptJson, analysisList, userId, riskType);

        String finalText = "🧠 투자 성향 기반 추천드릴게요!\n\n" + gptJson;
        return ExecutionResult.builder().finalContent(finalText).build();
    }

    private List<RecommendationStock> getTopVolumeStocks(int count) throws Exception {
        var raw = volumeRankingApi.getCombinedVolumeRanking(count, "0");
        return raw.stream()
                .map(ProfileStockMapper::fromMap)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(RecommendationStock::getCode, s -> s, (a, b) -> a),
                        m -> new ArrayList<>(m.values())));
    }

    private List<RecommendationStock> getDetailedStocks(List<RecommendationStock> stocks) {
        var tickers = stocks.stream().map(RecommendationStock::getCode).toList();
        var names = stocks.stream().map(RecommendationStock::getName).toList();
        return profileStockRecommender.getRecommendedStocksByProfile(tickers, names);
    }

    private static boolean positive(Double v) {
        return v != null && !v.isNaN() && !v.isInfinite() && v > 0.0;
    }
    private static List<RecommendationStock> filterByDefault(List<RecommendationStock> list) {
        return list.stream()
                .filter(s -> positive(s.getPer()))
                .filter(s -> positive(s.getPbr()))
                .filter(s -> positive(s.getRoe()))
                .filter(s -> positive(s.getEps()))
                .filter(s -> positive(s.getVolume()))
                .filter(s -> positive(s.getPrice()))
                .toList();
    }

    private void parseAndSaveRecommendations(
            String gptJson, List<ChatAnalysisDto> stockList, Integer userId, String riskType) {
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
                        .recommendType(IntentType.RECOMMEND_PROFILE.name())
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