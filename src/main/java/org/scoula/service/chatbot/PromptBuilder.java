package org.scoula.service.chatbot;

import org.scoula.domain.chatbot.dto.ChatAnalysisDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    // 투자 성향 기반 추천
    public String buildForProfile(Integer userId, String summary, List<ChatAnalysisDto> analysisList) {
        // analysisList 내용을 바탕으로 프롬프트 생성
        StringBuilder sb = new StringBuilder();
        sb.append("당신의 투자 성향은 다음과 같습니다: ").append(summary).append("\n\n");
        sb.append("다음은 성향에 맞는 추천 종목입니다:\n");

        for (ChatAnalysisDto stock : analysisList) {
            sb.append("- ").append(stock.getName())
                    .append(": 현재가 ").append(stock.getPrice())
                    .append(", PER ").append(stock.getPer())
                    .append(", 거래량 ").append(stock.getVolume())
                    .append("\n");
        }

        return sb.toString();
    }



    // 키워드 기반 추천
    public String buildForKeyword(String keyword) {
        return """
        다음 키워드와 관련된 종목을 추천해주세요:

        - 키워드: %s
        - 기준: 테마 관련성, 산업 동향, 시장 전망
        - 국내외 주식 각각 2개씩 추천
        - 각 종목은 [종목명, 간단한 추천 사유] 형태

        """.formatted(keyword);
    }

    // 종목 분석 요청
    public String buildForAnalysis(String stockName) {
        return """
        아래 종목에 대한 종합 분석을 해주세요:

        - 종목명: %s

        항목:
        1. 기업 개요
        2. 재무 지표 (PER, ROE, EPS 등)
        3. 성장성/시장 점유율
        4. 기술적 분석 요약
        5. 주요 리스크
        6. 종합 의견

        """.formatted(stockName);
    }

    // 모의투자 성과 분석
    public String buildForPortfolioAnalysis(Integer userId) {
        return """
        사용자 ID %d의 모의투자 내역을 기반으로 투자 성과를 분석해주세요.

        포함 항목:
        - 전체 수익률
        - 보유 종목 수
        - 매수/매도 빈도
        - 투자 전략의 일관성
        - 리스크 노출도
        - 개선점 및 피드백 요약

        """.formatted(userId);
    }

    // 세션 종료
    public String buildForSessionEnd() {
        return "대화를 종료합니다. 언제든지 다시 질문해주세요 😊";
    }

    // 에러 메시지 대응
    public String buildForError() {
        return "입력에 오류가 있어 처리를 할 수 없습니다. 다시 시도해주세요.";
    }

    // 알 수 없는 요청
    public String buildForUnknown() {
        return "요청을 이해하지 못했습니다. 다른 질문을 해보세요.";
    }

}
