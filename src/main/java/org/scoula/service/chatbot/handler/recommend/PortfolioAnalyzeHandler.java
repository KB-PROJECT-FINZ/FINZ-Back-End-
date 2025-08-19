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

/**
 * PortfolioAnalyzeHandler
 *
 * <p>IntentType.PORTFOLIO_ANALYZE 상황에서 실행되는 포트폴리오 분석 전용 핸들러.</p>
 *
 * <h3>흐름</h3>
 * <ol>
 *   <li>사용자 메시지에서 분석 기간 추출 (기본 30일)</li>
 *   <li>TradingService로부터 거래 통계(BehaviorStats) 조회</li>
 *   <li>프롬프트 생성 후 GPT 호출 → 투자 행동 분석 결과 획득</li>
 *   <li>JSON 파싱 → ChatBehaviorFeedbackDto로 매핑</li>
 *   <li>DB 저장: behavior_feedback, feedback_transaction 테이블에 기록</li>
 *   <li>최종 분석 결과를 ExecutionResult로 반환</li>
 * </ol>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>거래 내역이 없으면 안내 메시지 반환</li>
 *   <li>분석 결과를 JSON 파싱 실패 시 에러 안내</li>
 *   <li>@Transactional로 DB 저장(분석 결과 + 연관 거래) 원자성 보장</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioAnalyzeHandler implements IntentHandler {

    private final TradingService tradingService;
    private final PromptBuilder promptBuilder;
    private final org.scoula.util.chatbot.OpenAiClient openAiClient;
    private final ChatBotMapper chatBotMapper;

    /** 이 핸들러가 지원하는 Intent 타입 반환 → PORTFOLIO_ANALYZE */
    @Override public IntentType supports() { return IntentType.PORTFOLIO_ANALYZE; }

    /**
     * 포트폴리오 분석 처리
     * - 사용자 투자 기간별 거래 습관 분석 → GPT 호출 → JSON 파싱 → DB 저장 → 결과 반환
     */
    @Override
    @Transactional
    public ExecutionResult handle(ExecutionContext ctx) throws Exception {
        String userMsg = ctx.getUserMessage();

        // 1) 사용자 메시지에서 분석 기간(일 단위) 추출
        int requestedPeriod = extractPeriodDays(userMsg);

        // 2) 거래 통계 조회
        var stats = tradingService.getBehaviorStats(ctx.getUserId(), requestedPeriod);
        if (stats == null) {
            return ExecutionResult.builder()
                    .finalContent("📊 선택한 기간 동안 거래 내역이 없습니다.")
                    .requestedPeriod(requestedPeriod)
                    .build();
        }

        // 3) GPT 프롬프트 생성 + 호출
        String prompt = promptBuilder.buildForPortfolioAnalysis(stats);
        String content = openAiClient.getChatCompletion(prompt);

        // 4) GPT 응답(JSON) → DTO 파싱
        ChatBehaviorFeedbackDto parsed = parseBehaviorFeedback(content);
        if (parsed == null) {
            return ExecutionResult.builder()
                    .finalContent("❌ 분석 결과를 파싱하는 중 문제가 발생했습니다. 형식을 확인해주세요.")
                    .requestedPeriod(requestedPeriod)
                    .build();
        }

        // 5) 분석 결과 DTO에 기본 메타데이터 추가
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

        // 6) 분석 결과 저장 (chat_behavior_feedback 테이블)
        chatBotMapper.insertChatBehaviorFeedback(parsed);

        // 7) 분석 기간 내 관련 거래들을 feedback_transaction 테이블에 매핑 저장
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

        // 8) 최종 분석 결과 반환
        return ExecutionResult.builder()
                .finalContent(content)
                .requestedPeriod(requestedPeriod)
                .build();
    }

    /** 사용자 메시지에서 분석 기간을 추출 (기본값: 30일) */
    private int extractPeriodDays(String message) {
        if (message == null) return 30;
        if (message.contains("6개월")) return 180;
        if (message.contains("3개월")) return 90;
        if (message.contains("1개월")) return 30;
        return 30;
    }

    /** GPT JSON 응답을 ChatBehaviorFeedbackDto로 파싱 */
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
