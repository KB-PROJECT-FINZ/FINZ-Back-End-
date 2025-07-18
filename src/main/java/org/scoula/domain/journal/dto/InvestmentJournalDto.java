package org.scoula.domain.journal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.journal.vo.InvestmentJournalVO;

import java.time.LocalDate;
import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentJournalDto {
    private Integer id;
    private String emotion;
    private String reason;
    private String mistake;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date journalDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date updatedAt;


    private Integer userId;

    public static InvestmentJournalDto of(InvestmentJournalVO vo) {
        return vo == null ? null : InvestmentJournalDto.builder()
                .id(vo.getId())
                .journalDate(vo.getJournalDate())
                .emotion(vo.getEmotion())
                .reason(vo.getReason())
                .mistake(vo.getMistake())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .userId(vo.getUserId())
                .build();
    }

    public InvestmentJournalVO toVo() {
        return InvestmentJournalVO.builder()
                .id(id)
                .journalDate(journalDate)
                .emotion(emotion)
                .reason(reason)
                .mistake(mistake)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .userId(userId)
                .build();
    }
}
