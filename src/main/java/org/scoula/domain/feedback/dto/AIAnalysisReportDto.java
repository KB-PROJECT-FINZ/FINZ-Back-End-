package org.scoula.domain.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 분석 리포트 DTO
 * DB 테이블: chat_behavior_feedback
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIAnalysisReportDto {
    private Integer id;
    private Integer userId;
    private Integer sessionId;
    private Integer messageId;
    private String summaryText;       // 투자 전략의 특징
    private String riskText;          // 리스크 요인 및 개선점
    private String suggestionText;    // 개인 맞춤 조언
    private Integer transactionCount; // 거래 건수
    private Integer analysisPeriodDays; // 분석 기간 (일)
    private LocalDate startDate;      // 분석 시작일
    private LocalDate endDate;        // 분석 종료일
    private LocalDateTime generatedAt; // 생성일시
}