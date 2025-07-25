package org.scoula.service.journal;

import org.scoula.domain.journal.dto.InvestmentJournalDto;

import java.util.List;

public interface InvestmentJournalService {
    void createJournal(InvestmentJournalDto dto);
    List<InvestmentJournalDto> getJournalsByUserId(int userId);
    InvestmentJournalDto getJournalById(int id);
    void updateJournal(InvestmentJournalDto dto);
    void deleteJournal(int id);

}
