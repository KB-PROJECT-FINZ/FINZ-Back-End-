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

/**
 * RecommendProfileHandler
 *
 * <p>IntentType.RECOMMEND_PROFILE: 투자 성향(riskType)을 기반으로 종목을 추천하는 핸들러.</p>
 *
 * <h3>전체 흐름</h3>
 * <ol>
 *   <li>사용자 riskType 조회</li>
 *   <li>거래량 상위 종목 수집(VolumeRankingApi) → 중복 제거</li>
 *   <li>모의투자/DB 기반 상세 지표 결합(ProfileStockRecommender)</li>
 *   <li>기본 지표 필터(PER/PBR/ROE/EPS/거래량/가격) 적용</li>
 *   <li>fallback(비었을 때) 규칙으로 최소 후보 확보</li>
 *   <li>성향별 가이드라인(ProfileStockFilter)로 최종 후보 5개 선택</li>
 *   <li>분석 DTO 저장(chat_analysis), GPT 인사이트 생성 → 추천 저장(chat_recommendations)</li>
 *   <li>최종 결과 텍스트 반환</li>
 * </ol>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>룰 기반 1차 필터 + 성향 기반 2차 선택으로 품질/일관성 확보</li>
 *   <li>외부 API 결과를 내부 프로필/지표로 재정렬하여 개인화</li>
 *   <li>@Transactional로 분석/추천 저장의 원자성 보장</li>
 * </ul>
 */
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

    /** 지원 Intent 타입: RECOMMEND_PROFILE */
    @Override public IntentType supports() { return IntentType.RECOMMEND_PROFILE; }

    /**
     * 성향 기반 종목 추천 메인 처리.
     * <p>
     * - 상위 거래량 종목 수집 → 상세지표 결합 → 1차 필터 → (fallback) → 성향별 최종 5개 선정
     * - 선정 종목 분석 저장 → GPT 인사이트 생성 → 추천 저장 → 응답 반환
     * </p>
     */
    @Override
    @Transactional
    public ExecutionResult handle(ExecutionContext ctx) throws Exception {
        Integer userId = ctx.getUserId();
        String riskType = userProfileService.getRiskTypeByUserId(userId);

        // 1) 거래량 상위 종목 50개 수집(중복 제거 포함)
        var top = getTopVolumeStocks(50);
        // 2) 상세 지표 결합 (가격/지표/거래량 등 도메인 속성 보강)
        var detailed = getDetailedStocks(top);
        // 3) 기본 지표 필터링 (지표 유효성/범위 체크)
        var valid = filterByDefault(detailed);

        // 4) fallback: 기본 필터 결과가 비면, 완화된 positive 조건으로 상위 5개 보장
        if (valid.isEmpty()) {
            valid = detailed.stream()
                    .filter(s -> positive(s.getPer()) && positive(s.getPbr())
                            && positive(s.getRoe()) && positive(s.getEps())
                            && positive(s.getVolume()) && positive(s.getPrice()))
                    .sorted(Comparator.comparingDouble(RecommendationStock::getVolume).reversed())
                    .limit(5)
                    .toList();
        }

        // 5) 성향 기반 최종 후보 5개 선별 (리스크 선호도에 따른 필터/정렬)
        var filtered = ProfileStockFilter.selectByRiskType(riskType, valid, 5);

        // 6) 분석 결과 저장(chat_analysis)
        var analysisList = filtered.stream().map(ChatAnalysisMapper::toDto).toList();
        analysisList.forEach(chatBotMapper::insertAnalysis);

        // 7) GPT 인사이트 생성 (요약/추천 사유 등)
        String prompt = promptBuilder.buildForStockInsights(analysisList);
        String gptJson = openAiClient.getChatCompletion(prompt);

        // 8) 추천 저장(chat_recommendations)
        parseAndSaveRecommendations(gptJson, analysisList, userId, riskType);

        // 9) 최종 응답 메시지 구성
        String finalText = "🧠 투자 성향 기반 추천드릴게요!\n\n" + gptJson;
        return ExecutionResult.builder().finalContent(finalText).build();
    }

    /**
     * 거래량 상위 종목 수집 및 중복 제거.
     * <p>
     * - VolumeRankingApi에서 count개 랭킹을 가져오고
     * - 코드(code) 기준으로 중복 제거 후 리스트화
     * </p>
     */
    private List<RecommendationStock> getTopVolumeStocks(int count) throws Exception {
        var raw = volumeRankingApi.getCombinedVolumeRanking(count, "0");
        return raw.stream()
                .map(ProfileStockMapper::fromMap)// 외부 응답 → 내부 RecommendationStock 매핑
                .collect(Collectors.collectingAndThen(
                        // code(티커) 기준 중복 제거
                        Collectors.toMap(RecommendationStock::getCode, s -> s, (a, b) -> a),
                        m -> new ArrayList<>(m.values())));
    }

    /** 모의투자/DB를 통해 종목 상세 지표 보강 */
    private List<RecommendationStock> getDetailedStocks(List<RecommendationStock> stocks) {
        var tickers = stocks.stream().map(RecommendationStock::getCode).toList();
        var names = stocks.stream().map(RecommendationStock::getName).toList();
        return profileStockRecommender.getRecommendedStocksByProfile(tickers, names);
    }

    /** Double 값이 유효하고 양수인지 검사 */
    private static boolean positive(Double v) {
        return v != null && !v.isNaN() && !v.isInfinite() && v > 0.0;
    }

    /** 1차 기본 필터: 지표 유효성 위주로 걸러냄 */
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

    /**
     * GPT JSON 인사이트를 파싱하여 추천을 저장.
     * <p>
     * - GPT에서 제공한 ticker가 분석 리스트에 포함된 경우만 저장
     * - 추천 타입은 RECOMMEND_PROFILE로 마킹
     * </p>
     */
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