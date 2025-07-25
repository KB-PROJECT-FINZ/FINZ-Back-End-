package org.scoula.service.chatbot;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {
    
    // TEST용 입니다~~ 나중에 프롬프트 구체화 예정(토큰 절약)
    public String buildForProfile(String userId) {
        return "다음 사용자 ID 기반으로 종목 추천해줘: " + userId;
    }

    public String buildForKeyword(String keyword) {
        return "다음 키워드 기반으로 종목 추천해줘: " + keyword;
    }

    public String buildForAnalysis(String stockName) {
        return stockName + "의 재무/성장성/리스크 관점 종합 분석해줘";
    }
}
