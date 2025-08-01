package org.scoula.domain.chatbot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BehaviorStatsDto {
    private int transactionCount;     // 전체 거래 수
    private int analysisPeriod;       // 분석 기간 (일)
    private String startDate;         // 시작 날짜 (yyyy-MM-dd)
    private String endDate;           // 종료 날짜
    private double totalReturn;       // 총 수익률

    private int buyCount;             // 매수 횟수
    private int sellCount;            // 매도 횟수
    private double avgHoldDays;       // 평균 보유일
}