package org.scoula.domain.feedback.dto;

import lombok.Builder;
import lombok.Data;

import java.sql.Date;

@Data
@Builder
public class FeedbackSaveDto {
    private int userId;
    private Date weekStart;
    private Date weekEnd;
    private String feedback;
}
