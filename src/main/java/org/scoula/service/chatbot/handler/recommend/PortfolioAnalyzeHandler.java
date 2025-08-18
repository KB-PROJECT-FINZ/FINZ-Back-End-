package org.scoula.service.chatbot.handler.recommend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.domain.chatbot.dto.ChatBehaviorFeedbackDto;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.domain.trading.dto.TransactionDTO;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.scoula.service.chatbot.PromptBuilder;
import org.scoula.service.chatbot.handler.IntentHandler;
import org.scoula.service.chatbot.handler.context.ExecutionContext;
import org.scoula.service.chatbot.handler.result.ExecutionResult;
import org.scoula.service.trading.TradingService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioAnalyzeHandler implements IntentHandler {

    private final TradingService tradingService;
    private final PromptBuilder promptBuilder;
    private final org.scoula.util.chatbot.OpenAiClient openAiClient;
    private final ChatBotMapper chatBotMapper;

    @Override public IntentType supports() { return IntentType.PORTFOLIO_ANALYZE; }

    @Override
    @Transactional
    public ExecutionResult handle(ExecutionContext ctx) throws Exception {
        String userMsg = ctx.getUserMessage();
        int requestedPeriod = extractPeriodDays(userMsg);

        var stats = tradingService.getBehaviorStats(ctx.getUserId(), requestedPeriod);
        if (stats == null) {
            return ExecutionResult.builder()
                    .finalContent("📊 선택한 기간 동안 거래 내역이 없습니다.")
                    .requestedPeriod(requestedPeriod)
                    .build();
        }

        String prompt = promptBuilder.buildForPortfolioAnalysis(stats);
        String content = openAiClient.getChatCompletion(prompt);

        // 파싱
        ChatBehaviorFeedbackDto parsed = parseBehaviorFeedback(content);
        if (parsed == null) {
            return ExecutionResult.builder()
                    .finalContent("❌ 분석 결과를 파싱하는 중 문제가 발생했습니다. 형식을 확인해주세요.")
                    .requestedPeriod(requestedPeriod)
                    .build();
        }

        // 저장
        parsed.setUserId(ctx.getUserId());
        parsed.setSessionId(ctx.getSessionId());
        parsed.setTransactionCount(stats.getTransactionCount());



        if (ctx.getMessageId() == null) {
            throw new IllegalStateException("messageId가 없습니다. 컨텍스트 전달을 확인하세요.");
        }
        parsed.setMessageId(ctx.getMessageId());

        // INT/DATE 컬럼은 정확한 타입으로 세팅
        parsed.setAnalysisPeriod(requestedPeriod);
        parsed.setAnalysisStart(stats.getAnalysisStart());
        parsed.setAnalysisEnd(stats.getAnalysisEnd());

        // 한 번만 insert
        chatBotMapper.insertChatBehaviorFeedback(parsed);

        // 연관 거래 저장
        List<TransactionDTO> txs = tradingService.getUserTransactions(ctx.getUserId());
        txs.sort(Comparator.comparing(TransactionDTO::getExecutedAt));

        List<Long> relatedIds = txs.stream()
                .filter(tx -> {
                    LocalDate d = tx.getExecutedAt().toLocalDate();
                    return !d.isBefore(stats.getAnalysisStart()) && !d.isAfter(stats.getAnalysisEnd());
                })
                .map(tx -> (long) tx.getTransactionId()).toList();

        for (Long id : relatedIds) {
            chatBotMapper.insertChatBehaviorFeedbackTransaction(parsed.getId(), id);
        }

        return ExecutionResult.builder()
                .finalContent(content)
                .requestedPeriod(requestedPeriod)
                .build();
    }

    private int extractPeriodDays(String message) {
        if (message == null) return 30;
        if (message.contains("6개월")) return 180;
        if (message.contains("3개월")) return 90;
        if (message.contains("1개월")) return 30;
        return 30;
    }

    private ChatBehaviorFeedbackDto parseBehaviorFeedback(String content) {
        try {
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            return mapper.readValue(content, ChatBehaviorFeedbackDto.class);
        } catch (Exception e) {
            log.error("파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
