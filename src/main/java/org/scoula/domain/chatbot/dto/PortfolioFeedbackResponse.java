package org.scoula.domain.chatbot.dto;

import lombok.Data;

@Data
public class PortfolioFeedbackResponse {
    private int periodDays;
    private String startDate;
    private String endDate;
    private String strategySummary;
    private String riskPoint;
    private String suggestion;
}