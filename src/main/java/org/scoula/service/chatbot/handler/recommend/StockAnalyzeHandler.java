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

@Component
@RequiredArgsConstructor
public class StockAnalyzeHandler implements IntentHandler {

    private final StockNameParser stockNameParser;
    private final ProfileStockRecommender profileStockRecommender;
    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;

    @Override public IntentType supports() { return IntentType.STOCK_ANALYZE; }

    @Override
    public ExecutionResult handle(ExecutionContext ctx) throws Exception {
        String prompt = promptBuilder.stockextractionPrompt(ctx.getUserMessage());
        String gptResponse = openAiClient.getChatCompletion(prompt);

        StockExtractionResultDto result = stockNameParser.parseStockExtraction(gptResponse);
        if (result.getStockName() == null || result.getStockName().isBlank()) {
            return ExecutionResult.builder().finalContent("❌ 종목명을 정확히 입력해주세요.").build();
        }

        RecommendationStock raw = RecommendationStock.builder()
                .name(result.getStockName())
                .code(result.getTicker())
                .build();

        var detailed = getDetailedStocks(List.of(raw));
        if (detailed.isEmpty()) {
            return ExecutionResult.builder().finalContent("❌ 해당 종목의 상세 정보를 찾을 수 없습니다.").build();
        }

        var dto = ChatAnalysisMapper.toDto(detailed.get(0));

        String stockAnalysisPrompt = promptBuilder.buildForStockAnalysis(List.of(dto));
        String analysisText = openAiClient.getChatCompletion(stockAnalysisPrompt);

        String finalText = "🔍 종목 분석 결과입니다.\n\n" + analysisText;
        return ExecutionResult.builder().finalContent(finalText).build();
    }

    private List<RecommendationStock> getDetailedStocks(List<RecommendationStock> stocks) {
        var tickers = stocks.stream().map(RecommendationStock::getCode).toList();
        var names = stocks.stream().map(RecommendationStock::getName).toList();
        return profileStockRecommender.getRecommendedStocksByProfile(tickers, names);
    }
}
