package org.scoula.service.chatbot.handler.recommend;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.dto.RecommendationStock;
import org.scoula.domain.chatbot.dto.StockExtractionResultDto;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.service.chatbot.ProfileStockRecommender;
import org.scoula.service.chatbot.PromptBuilder;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.scoula.util.chatbot.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * StockAnalyzeHandler
 *
 * <p>IntentType.STOCK_ANALYZE: 단일 종목에 대한 분석을 수행하는 핸들러.</p>
 *
 * <h3>전체 흐름</h3>
 * <ol>
 *   <li>사용자 메시지에서 GPT로 종목명/티커 추출</li>
 *   <li>추출한 코드/이름으로 상세 지표 조회(ProfileStockRecommender)</li>
 *   <li>상세 지표를 기반으로 분석 프롬프트 생성 → GPT 분석</li>
 *   <li>분석 결과를 자연어로 반환</li>
 * </ol>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>입력 유효성 검사: 종목명/상세 지표 없을 때 즉시 안내</li>
 *   <li>도메인 데이터(가격/재무/거래량 등)를 묶어 GPT에 근거로 제공</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StockAnalyzeHandler implements IntentHandler {

    private final StockNameParser stockNameParser;
    private final ProfileStockRecommender profileStockRecommender;
    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;

    /** 지원 Intent 타입: STOCK_ANALYZE */
    @Override public IntentType supports() { return IntentType.STOCK_ANALYZE; }

    /**
     * 단건 종목 분석 처리.
     * <p>
     * 1) 메시지 → (프롬프트) → GPT로 종목명/티커 추출<br/>
     * 2) 상세 지표 조회 실패 시 안내 후 종료<br/>
     * 3) 상세 지표 → 분석 프롬프트 생성 → GPT 분석 결과 수신<br/>
     * 4) 자연어 텍스트로 사용자에게 반환
     * </p>
     *
     * @param ctx 실행 컨텍스트 (userId, sessionId, userMessage 등)
     * @return 분석 결과 텍스트를 담은 ExecutionResult
     */
    @Override
    public ExecutionResult handle(ExecutionContext ctx) throws Exception {
        // 1) 종목명/티커 추출 프롬프트 생성 및 호출
        String prompt = promptBuilder.stockextractionPrompt(ctx.getUserMessage());
        String gptResponse = openAiClient.getChatCompletion(prompt);

        // 2) GPT 응답 파싱 → StockExtractionResultDto
        StockExtractionResultDto result = stockNameParser.parseStockExtraction(gptResponse);

        // 3) 유효성 검사: 종목명이 없으면 즉시 안내
        if (result.getStockName() == null || result.getStockName().isBlank()) {
            return ExecutionResult.builder().finalContent("❌ 종목명을 정확히 입력해주세요.").build();
        }

        // 4) 최소 정보로 기본 종목 객체 구성 (코드/이름)
        RecommendationStock raw = RecommendationStock.builder()
                .name(result.getStockName())
                .code(result.getTicker())
                .build();

        // 5) 상세 지표 조회 (재무/거래/가격 등)
        var detailed = getDetailedStocks(List.of(raw));
        if (detailed.isEmpty()) {
            return ExecutionResult.builder().finalContent("❌ 해당 종목의 상세 정보를 찾을 수 없습니다.").build();
        }

        // 6) 상세 지표 → 분석용 DTO로 변환
        var dto = ChatAnalysisMapper.toDto(detailed.get(0));

        // 7) 분석 프롬프트 생성 후 GPT 호출
        String stockAnalysisPrompt = promptBuilder.buildForStockAnalysis(List.of(dto));
        String analysisText = openAiClient.getChatCompletion(stockAnalysisPrompt);

        // 8) 최종 결과 텍스트 구성 및 반환
        String finalText = "🔍 종목 분석 결과입니다.\n\n" + analysisText;
        return ExecutionResult.builder().finalContent(finalText).build();
    }

    /**
     * 상세 종목 데이터 조회 유틸.
     * <p>입력 받은 종목 리스트에서 코드/이름 배열을 뽑아 ProfileStockRecommender 호출.</p>
     */
    private List<RecommendationStock> getDetailedStocks(List<RecommendationStock> stocks) {
        var tickers = stocks.stream().map(RecommendationStock::getCode).toList();
        var names = stocks.stream().map(RecommendationStock::getName).toList();
        return profileStockRecommender.getRecommendedStocksByProfile(tickers, names);
    }
}
