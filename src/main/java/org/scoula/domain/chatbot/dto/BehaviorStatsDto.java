package org.scoula.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BehaviorStatsDto {
    private int transactionCount;     // 전체 거래 수
    private double totalReturn;       // 총 수익률 (%)
    private int buyCount;             // 매수 횟수
    private int sellCount;            // 매도 횟수
    private double avgHoldDays;       // 평균 보유일 (추후 구현 필요)
    private int requestedPeriod;      // 사용자 요청 분석 기간 (일)

}
