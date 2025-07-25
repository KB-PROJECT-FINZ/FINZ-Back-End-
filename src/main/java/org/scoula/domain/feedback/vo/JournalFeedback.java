package org.scoula.domain.feedback.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalFeedback {
    private Integer id;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date weekStart;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date weekEnd;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date generatedAt;
    private Integer userId;
    private String feedback;
}
