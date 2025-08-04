package org.scoula.domain.chatbot.enums;

public enum IntentType {
    MESSAGE, // 잡담, 일반 대화 포함
    RECOMMEND_PROFILE, //프로필 기반 추천
    RECOMMEND_KEYWORD, //키워드 기반 추천
    STOCK_ANALYZE, // 종목 분석
    PORTFOLIO_ANALYZE, //모의투자 분석 피드백
    TERM_EXPLAIN, //용어 설명
    SESSION_END, //대화 종료 요청
    ERROR, // 에러
    UNKNOWN // 의도 분류 실패 
}
