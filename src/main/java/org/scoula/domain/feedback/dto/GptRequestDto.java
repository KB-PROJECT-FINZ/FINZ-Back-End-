package org.scoula.domain.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.scoula.domain.journal.vo.InvestmentJournalVO;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GptRequestDto {
    private List<InvestmentJournalVO> journals;
}
