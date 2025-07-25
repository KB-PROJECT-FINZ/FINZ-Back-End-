package org.scoula.service.feedback;

import org.scoula.domain.feedback.dto.GptRequestDto;
import org.scoula.domain.journal.vo.InvestmentJournalVO;
import java.util.List;

public interface GptService {
    String callGpt(GptRequestDto requestDto);
}
