package org.scoula.service.chatbot;

import org.scoula.domain.chatbot.dto.ChatAnalysisDto;
import org.scoula.domain.chatbot.dto.ChatRecommendationDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    // 분석용 프롬프트
    public String buildForStockInsights(List<ChatAnalysisDto> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
    다음은 사용자의 투자 성향에 기반하여 선별된 종목들의 상세 데이터입니다.
    아래 데이터를 참고하여 각 종목에 대해 아래 항목을 포함한 투자 분석 의견을 작성하세요:
    
                톤앤매너 지시사항:
                - 말투는 친근하고 부드럽게 해주세요.
                - ‘~해요’, ‘~있어요’, ‘~보여요’ 등 **토스처럼 자연스럽고 신뢰감 있는 말투**를 사용해 주세요.
                - “추천합니다”처럼 단정적인 표현은 피하고, “이런 분석이 가능해요”, “관심 가져볼 수 있어요”처럼 **제안형 어투**를 사용해 주세요.

    1. 종목별 핵심 투자 포인트
    2. 주목할 기술적/재무적 지표 해석
    3. 사용자의 투자 성향(위험 선호도, 투자 기간 등)과의 적합성 이유
    4. 리스크 수준 평가 (낮음 / 중간 / 높음)
    5. 향후 3~6개월 간 긍정적/부정적 시나리오 요약
    6. 매수 시점에 대한 참고 코멘트 (예: 지금은 관망, 조정 시 진입 고려 등)
    7. 해당 종목의 산업/섹터 내 위치 및 경쟁력 요약
    8. 투자 판단 시 유의해야 할 외부 요인 또는 리스크 요인

    - 반드시 출력은 JSON 배열 형식으로 하며, 각 종목은 다음 구조를 따라야 하며 모든 필드 간 쉼표(,)를 포함하여 **정확한 JSON 문법**을 지켜야 합니다.. :
    ```json
    {
      "ticker": "005930",
      "reason": "삼성전자는 AI 반도체 시장 확대와 수익성 개선 기대감에 따라 성장 가능성이 **있어 보여요**. PER이 낮고 ROE가 양호해서 **투자 매력이 있어요**. 사용자의 단기/공격형 성향과도 **잘 어울려요**."
      "riskLevel": "중간",
      "timingComment": "최근 조정 이후 기술적으로 안정화 구간. 분할 매수 고려 가능",
      "futureOutlook": "AI 수요 증가로 인한 실적 개선 기대. 단, 글로벌 경기 둔화는 주의 필요"
    }
    ```

    분석은 개인적 의견 형태로 작성하고, 투자 권유 표현은 지양하세요.
    예: “추천합니다” 대신 “이런 분석이 가능합니다”, “진입 고려 가능성 있음” 등의 표현 사용.
    """);

        for (ChatAnalysisDto s : list) {
            sb.append("- ").append(s.getName())
                    .append(" (").append(s.getTicker()).append(")\n")
                    .append("  • 현재가: ").append(s.getPrice()).append("원\n")
                    .append("  • PER: ").append(s.getPer()).append(", ROE: ").append(s.getRoe()).append(", EPS: ").append(s.getEps()).append("\n")
                    .append("  • PBR: ").append(s.getPbr()).append(", 가중평균가: ").append(s.getAvgPrice()).append("\n")
                    .append("  • 시가/고가/저가: ").append(s.getOpen()).append(" / ").append(s.getHigh()).append(" / ").append(s.getLow()).append("\n")
                    .append("  • 52주 고가/저가: ").append(s.getHigh52w()).append(" / ").append(s.getLow52w()).append("\n")
                    .append("  • 거래량: ").append(s.getVolume()).append(", 회전율: ").append(s.getTurnRate()).append("%, 외국인 보유율: ").append(s.getForeignRate()).append("%\n\n");
        }

        return sb.toString();
    }

    // 분석한 제이슨 + 이유 + 신버전  투자 성향 기반 추천  응답 프롬프트
    public String buildSummaryFromRecommendations(String summary, List<ChatRecommendationDto> recList, List<ChatAnalysisDto> analysisList) {
        StringBuilder sb = new StringBuilder();

        sb.append("아래는 사용자의 투자 성향과 이에 맞춰 선별된 종목 정보입니다.\n")
                .append("사용자의 투자 성향 요약: ").append(summary).append("\n\n")
                .append("각 종목의 GPT 추천 사유와 수치 데이터(PER, ROE, 외인율 등)를 참고하여\n")
                .append("다음 조건을 만족하는 자연스러운 요약을 생성해주세요:\n")
                .append("- 각 종목마다 한 문단 정도의 요약 문장 (대상자의 눈높이에 맞게)\n")
                .append("- 수치 해석(PER 낮음 → 저평가, ROE 높음 → 수익성 좋음 등)은 자연스럽게 녹여주세요\n")
                .append("- '지금 매수하라' 같은 표현은 피하고, 참고/분석/가능성 위주로 말해주세요\n")
                .append("- 말투는 신뢰감 있으면서도 따뜻하게. (ex. ~로 해석돼요, ~일 수 있어요 등)\n\n");

        for (ChatRecommendationDto rec : recList) {
            ChatAnalysisDto stat = analysisList.stream()
                    .filter(s -> s.getTicker().equals(rec.getTicker()))
                    .findFirst().orElse(null);
            if (stat == null) continue;

            sb.append("📌 종목명: ").append(stat.getName()).append(" (").append(stat.getTicker()).append(")\n");
            sb.append("• GPT 분석: ").append(rec.getReason()).append("\n");
            sb.append("• 현재가: ").append(stat.getPrice()).append("원\n");
            sb.append("• PER: ").append(stat.getPer()).append(", ROE: ").append(stat.getRoe()).append(", EPS: ").append(stat.getEps()).append("\n");
            sb.append("• PBR: ").append(stat.getPbr()).append(", 평균 매입가: ").append(stat.getAvgPrice()).append("원\n");
            sb.append("• 외인 보유율: ").append(stat.getForeignRate()).append("%, 거래 회전율: ").append(stat.getTurnRate()).append("%\n\n");
        }

        sb.append("위 정보를 바탕으로 종목별로 자연어 요약을 생성해주세요.");

        return sb.toString();
    }





    // 투자 성향 기반 추천 구버전
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

    // 용어 설명
    public String buildForTermExplain(String term) {
        return """
    아래 투자 용어에 대해 설명해주세요:

    - 용어: %s
    - 포함할 항목:
      1. 정의 및 개념
      2. 투자 시 의미와 활용 예시
      3. 초보자 관점에서의 해석
    - 가능한 한 이해하기 쉽게 설명

    """.formatted(term);
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
