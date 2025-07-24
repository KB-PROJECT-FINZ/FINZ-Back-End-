package org.scoula.domain.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;

@Data
@AllArgsConstructor
public class FeedbackResponseDto {
    private int userId;
    private Date weekStart;
    private Date weekEnd;
    private String feedback;
    private String statusMessage;
}
