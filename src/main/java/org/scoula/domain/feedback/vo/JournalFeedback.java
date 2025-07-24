package org.scoula.domain.feedback.vo;

import lombok.Builder;
import lombok.Data;

import java.sql.Date;

@Data
@Builder
public class JournalFeedback {
    private Integer id;
    private Date weekStart;
    private Date weekEnd;
    private String feedback;
    private Integer userId;
}
