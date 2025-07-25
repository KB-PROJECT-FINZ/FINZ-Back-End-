package org.scoula.domain.journal.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class InvestmentJournalVO {

    private Integer id;
    private Date journalDate;
    private String emotion;
    private String reason;
    private String mistake;
    private Date createdAt;
    private Date updatedAt;
    private Integer userId;
}
