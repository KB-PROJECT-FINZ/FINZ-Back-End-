package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.scoula.domain.journal.vo.InvestmentJournalVO;

import java.util.List;

@Mapper
public interface InvestmentJournalMapper {
    void insertJournal(InvestmentJournalVO vo);
    InvestmentJournalVO findById(int id);
    List<InvestmentJournalVO> findByUserId(int userId);
    void updateJournal(InvestmentJournalVO vo);
    void deleteJournal(int id);
}
