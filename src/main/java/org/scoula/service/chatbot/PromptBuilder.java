package org.scoula.service.chatbot;

import org.scoula.domain.chatbot.dto.BehaviorStatsDto;
import org.scoula.domain.chatbot.dto.ChatAnalysisDto;
import org.scoula.domain.chatbot.dto.ChatRecommendationDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class PromptBuilder {

    /**
     *   투자 성향 기반 종목 분석 프롬프트
     * - 여러 종목의 재무/기술 지표 데이터를 기반으로 GPT에게 분석 요청
     * - 분석 시 "투자 권유"가 아닌 "분석/제안형" 어투를 사용하도록 제한
     * - 출력은 반드시 JSON 배열로 강제
     */
    public String buildForStockInsights(List<ChatAnalysisDto> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
    다음은 사용자의 투자 성향에 기반하여 선별된 종목들의 상세 데이터입니다.
    아래 데이터를 참고하여 각 종목에 대해 아래 항목을 포함한 투자 분석 의견을 작성하세요:
    
                톤앤매너 지시사항:
                - 말투는 친근하고 부드럽게 해주세요.
                - '~해요', '~있어요', '~보여요' 등 **토스처럼 자연스럽고 신뢰감 있는 말투**를 사용해 주세요.
                - "추천합니다"처럼 단정적인 표현은 피하고, "이런 분석이 가능해요", "관심 가져볼 수 있어요"처럼 **제안형 어투**를 사용해 주세요.

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
      "name": "삼성전자",
      "ticker": "005930",
      "reason": "삼성전자는 AI 반도체 시장 확대와 수익성 개선 기대감에 따라 성장 가능성이 **있어 보여요**. PER이 낮고 ROE가 양호해서 **투자 매력이 있어요**. 사용자의 단기/공격형 성향과도 **잘 어울려요**.",
      "riskLevel": "중간",
      "timingComment": "최근 조정 이후 기술적으로 안정화 구간. 분할 매수 고려 가능",
      "futureOutlook": "AI 수요 증가로 인한 실적 개선 기대. 단, 글로벌 경기 둔화는 주의 필요"
    }
    ```

    분석은 개인적 의견 형태로 작성하고, 투자 권유 표현은 지양하세요.
    예: "추천합니다" 대신 "이런 분석이 가능합니다", "진입 고려 가능성 있음" 등의 표현 사용.
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

    /**
     *   단일 종목 분석 프롬프트 (STOCK_ANALYZE 전용)
     * - 사용자가 특정 종목(삼성전자 등)에 대한 분석 요청 시 사용
     * - 위와 동일하게 JSON 배열 형식 강제
     */
    public String buildForStockAnalysis(List<ChatAnalysisDto> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
    다음은 사용자가 요청한 종목의 상세 데이터입니다.
    아래 데이터를 참고하여 해당 종목에 대해 아래 항목을 포함한 투자 분석 의견을 작성하세요:
    
                톤앤매너 지시사항:
                - 말투는 친근하고 부드럽게 해주세요.
                - '~해요', '~있어요', '~보여요' 등 **토스처럼 자연스럽고 신뢰감 있는 말투**를 사용해 주세요.
                - "추천합니다"처럼 단정적인 표현은 피하고, "이런 분석이 가능해요", "관심 가져볼 수 있어요"처럼 **제안형 어투**를 사용해 주세요.

    1. 종목별 핵심 투자 포인트
    2. 주목할 기술적/재무적 지표 해석
    3. 리스크 수준 평가 (낮음 / 중간 / 높음)
    4. 향후 3~6개월 간 긍정적/부정적 시나리오 요약
    5. 매수 시점에 대한 참고 코멘트 (예: 지금은 관망, 조정 시 진입 고려 등)
    6. 해당 종목의 산업/섹터 내 위치 및 경쟁력 요약
    7. 투자 판단 시 유의해야 할 외부 요인 또는 리스크 요인

    - 반드시 출력은 JSON 배열 형식으로 하며, 각 종목은 다음 구조를 따라야 하며 모든 필드 간 쉼표(,)를 포함하여 **정확한 JSON 문법**을 지켜야 합니다.. :
    ```json
    {
      "ticker": "NAVER",
      "reason": "네이버는 국내 인터넷 서비스의 대표주자로 알려져 있고, 검색, 커머스, 콘텐츠 등 다양한 사업 영역을 보유하고 있어요. 기술적으로도 AI, 클라우드 등 미래 성장 동력을 확보하고 있어 관심 가져볼 수 있어요. 하지만 경쟁 구도 변화와 규제 리스크가 있어 중위험 중수익 종목이에요.",
      "riskLevel": "높음",
      "timingComment": "최근 주가 상승으로 매수시 적절한 타이밍이 아닐 수 있어요. 추가 하락 기회를 기다려보세요.",
      "futureOutlook": "AI 시장 성장에 따른 수요 증가가 기대되지만, 경쟁사와의 경쟁이 치열할 수 있어요."
    }
    ```

    분석은 개인적 의견 형태로 작성하고, 투자 권유 표현은 지양하세요.
    예: "추천합니다" 대신 "이런 분석이 가능합니다", "진입 고려 가능성 있음" 등의 표현 사용.
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

    /**
     *   성향 기반 종목 추천 요약 프롬프트
     * - GPT가 종목별 분석(JSON) → 자연스러운 한국어 요약으로 재가공하도록 요청
     * - 추천 사유 + 수치 데이터(PER, ROE 등)를 모두 포함
     */
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

            sb.append(" 종목명: ").append(stat.getName()).append(" (").append(stat.getTicker()).append(")\n");
            sb.append("• GPT 분석: ").append(rec.getReason()).append("\n");
            sb.append("• 현재가: ").append(stat.getPrice()).append("원\n");
            sb.append("• PER: ").append(stat.getPer()).append(", ROE: ").append(stat.getRoe()).append(", EPS: ").append(stat.getEps()).append("\n");
            sb.append("• PBR: ").append(stat.getPbr()).append(", 평균 매입가: ").append(stat.getAvgPrice()).append("원\n");
            sb.append("• 외인 보유율: ").append(stat.getForeignRate()).append("%, 거래 회전율: ").append(stat.getTurnRate()).append("%\n\n");
        }

        sb.append("위 정보를 바탕으로 종목별로 자연어 요약을 생성해주세요.");

        return sb.toString();
    }

    /**
     *   키워드 기반 종목 추천 프롬프트
     * - 특정 산업/테마/섹터(예: 반도체, AI) 기반 종목 추출
     * - 한국 주식시장(KOSPI/KOSDAQ/KONEX)만 허용
     * - 최소 5개 이상의 관련 종목을 JSON 배열로 반환하도록 강제
     */

    public String buildForKeyword(String keyword) {
        return """
        The user requested stock recommendations related to the keyword: "%s".

        Conditions:
        - Only recommend Korean stocks that are **clearly and directly related to the industry or sector of "%s"**.
        - **Do NOT include any stocks unrelated to "%s"** such as those in unrelated themes like biotech, secondary batteries, hydrogen vehicles, media/content, gaming, entertainment, or unlisted subsidiaries.
        - Give priority to companies that are part of the supply chain, key component manufacturers, or direct beneficiaries of "%s".
        - Exclude duplicate listings, preferred shares, and stocks that are only indirectly related.
        - Do not recommend stocks just because they are popular or trending — relevance must be based on industrial and factual relationships.
        - All recommended stocks must be listed on the Korean stock market (KOSPI, KOSDAQ, or KONEX).
        - **Stock names must be written in Korean only. Do NOT use English stock names.**
        - You **must recommend at least 5 different stocks** that strictly meet the above criteria.

        Output format (strictly JSON array only):
        [
          { "name": "종목명 (in Korean)", "code": "Ticker Code" },
          { "name": "종목명2 (in Korean)", "code": "Ticker Code" },
          ...
        ]

        Your output must be the raw JSON array only. No explanation, no comments, no formatting outside the JSON.
        """.formatted(keyword, keyword, keyword, keyword);
    }


    /**
     *   사용자 입력에서 종목명/티커 추출 프롬프트
     * - 입력 문장에서 첫 번째 언급된 종목만 추출
     * - 티커 없으면 null 반환
     */
    public String stockextractionPrompt(String userMessage) {
        return """
        Extract the stock name and its ticker symbol from the user input below.

        Format your output **exactly** as follows:
        Stock: <stock name>
        Ticker: <ticker symbol or null if unknown>

        Rules:
        - Return only the **first** stock mentioned, if there are multiple.
        - If you cannot find a valid ticker, write `null`.
        - The stock name must appear in the input. Do not guess new ones.
        - Do not include any explanations or additional text — just the 2 lines in the format.

        🧪 Examples:
        Input: 삼성전자 분석해줘
        Output:  
        Stock: 삼성전자  
        Ticker: 005930

        Input: 카카오 주가 어때?  
        Output:  
        Stock: 카카오  
        Ticker: 035720

        Input: 네이버 어때?  
        Output:  
        Stock: 네이버  
        Ticker: 035420

        Input: 삼성전자 어때?  
        Output:  
        Stock: 삼성전자  
        Ticker: 005930

        Input: 네이버 ㄱㅊ?  
        Output:  
        Stock: 네이버  
        Ticker: 035420

        Input: 카카오  
        Output:  
        Stock: 카카오  
        Ticker: 035720

        Input: 현대차 알려줘  
        Output:  
        Stock: 현대차  
        Ticker: 005380

        👉 Input: %s  
        Output:
        """.formatted(userMessage);
    }



    /**
     *   모의투자 거래 성과 분석 프롬프트
     * - 최근 n일 동안의 거래 데이터 기반 전략 요약, 리스크, 개선 제안
     * - JSON 구조 강제
     */
    public String buildForPortfolioAnalysis(BehaviorStatsDto stats) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(stats.getRequestedPeriod());
        return """
        사용자는 최근 %d일 동안의 거래에 대해 분석을 요청했습니다.

        📊 거래 요약
        - 총 거래 횟수: %d회
        - 총 수익률: %.2f%%
        
        🪙 거래 활동
        - 매수 횟수: %d회
        - 매도 횟수: %d회
        - 평균 보유일: %.2f일

        위 통계를 바탕으로 사용자의 투자 성향 및 전략에 대해 분석하고,
        개선점과 피드백을 요약해 주세요.

    
                📝 출력 형식 (정확히 이 구조로 JSON으로 반환하세요):
                {
                  "periodDays": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "strategySummary": "...",
                  "riskPoint": "...",
                  "suggestion": "..."
                }
        [주의사항]
        - 데이터가 적을 경우에도 의미 있는 조언을 해줘
        - 반복 표현(예: "더 많이 연구해야 한다")은 피하고, 구체적인 행동 중심으로 조언해줘
        - strategySummary에 수익률이 들어가야돼.
        """.formatted(
                stats.getRequestedPeriod(),
                stats.getTransactionCount(), stats.getTotalReturn(),
                stats.getBuyCount(), stats.getSellCount(), stats.getAvgHoldDays(),
                stats.getRequestedPeriod(),
                startDate.toString(), endDate.toString()
        );
    }

    /**
     *   투자 용어 설명 프롬프트
     * - 특정 용어(예: PER, EPS)를 초보자도 이해할 수 있게 JSON으로 설명
     */
    public String buildForTermExplain(String term) {
        return """
아래 투자 용어에 대해 JSON 형식으로 설명해주세요.

조건:
- JSON 구조는 다음과 같아야 합니다:

{
  "term": "용어명",
  "definition": "정의 및 개념",
  "meaning": "투자 시 의미와 활용 예시",
  "beginnerTip": "초보자 관점에서의 해석"
}

요청 내용:
- 용어: %s
- 각 항목은 반드시 포함
- 초보자도 이해할 수 있도록 간단하고 친절하게 작성

""".formatted(term);
    }

    /**
     *   키워드 추출 프롬프트
     * - 사용자 입력에서 산업/테마/섹터 키워드만 JSON으로 반환
     */
    public String buildKeywordExtractionPrompt(String userMessage) {
        return """
    You are a keyword extractor for a financial stock chatbot.

    From the following user message, extract the **main keyword** related to industry, sector, theme, or stock category.

    Your answer must be in the following JSON format only:
    {
      "keyword": "<extracted keyword>"
    }

    The keyword must be:
    - 1 to 3 words max
    - Relevant to finance, investment, or stocks
    - No explanation or comment

    Examples:

    User: "AI 관련된 주식 추천해줘"
    Answer: { "keyword": "AI" }

    User: "2차전지 관련 종목 뭐 있어?"
    Answer: { "keyword": "2차전지" }

    User: "친환경 에너지 테마주 알려줘"
    Answer: { "keyword": "친환경 에너지" }

    User: "전기차 관련 주식 뭐가 괜찮아?"
    Answer: { "keyword": "전기차" }

    User: "반도체 관련주 추천해줘"
    Answer: { "keyword": "반도체" }

    User: "우주항공 테마는 어때?"
    Answer: { "keyword": "우주항공" }

    User: "리츠 관련 종목 알려줘"
    Answer: { "keyword": "리츠" }

    User: "원자력 발전 관련된 기업 있어?"
    Answer: { "keyword": "원자력 발전" }

    User: "게임주 중에 좋은 거 있어?"
    Answer: { "keyword": "게임" }

    User: "은행주 어떻게 생각해?"
    Answer: { "keyword": "은행" }

    User: "해외 여행 수혜주 추천해줘"
    Answer: { "keyword": "여행" }

    User: "건설업종 중 괜찮은 회사 있어?"
    Answer: { "keyword": "건설" }

    User: "%s"
    """.formatted(userMessage);
    }

    /**
     *   의도 분류 프롬프트
     * - 사용자 입력을 IntentType(enum) 중 하나로 분류
     */
    public String buildIntentClassificationPrompt(String userMessage) {
        return """
    You are an intent classifier for a financial chatbot.
    
    Classify the user's message into one of the following intent types **based on the meaning**:
    
    - MESSAGE: General conversation or small talk.
    - RECOMMEND_PROFILE: Ask for stock recommendations based on investment profile.
    - RECOMMEND_KEYWORD: Ask for stock recommendations by keyword (e.g., AI-related stocks).
    - STOCK_ANALYZE: Ask for analysis of a specific stock (e.g., "Tell me about Samsung Electronics").
    - PORTFOLIO_ANALYZE: Ask to analyze the user's mock investment performance.
    - SESSION_END: Wants to end the conversation.
    - ERROR: Clear error or invalid message.
    - UNKNOWN: Cannot determine intent.
    
    Just return the intent type only, no explanation.

    Example 1:
    User: "AI 관련된 주식 추천해줘"
    Answer: RECOMMEND_KEYWORD

    Example 2:
    User: "내 투자 성향으로 추천해줘"
    Answer: RECOMMEND_PROFILE

    Example 3:
    User: "내 성향에 맞는 주식 뭐야?"
    Answer: RECOMMEND_PROFILE

    Example 4:
    User: "성향 기반으로 추천해줘"
    Answer: RECOMMEND_PROFILE

    Example 5:
    User: "삼성전자 분석해줘"
    Answer: STOCK_ANALYZE

    User: %s
    """.formatted(userMessage);
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
