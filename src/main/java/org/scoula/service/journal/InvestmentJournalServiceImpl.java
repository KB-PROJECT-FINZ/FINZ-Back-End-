package org.scoula.service.journal;

import org.scoula.domain.journal.dto.InvestmentJournalDto;
import org.scoula.domain.journal.vo.InvestmentJournalVO;
import org.scoula.mapper.InvestmentJournalMapper;
import org.scoula.service.journal.InvestmentJournalService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvestmentJournalServiceImpl implements InvestmentJournalService {

    private final InvestmentJournalMapper investmentJournalMapper;

    public InvestmentJournalServiceImpl(InvestmentJournalMapper investmentJournalMapper) {
        this.investmentJournalMapper = investmentJournalMapper;
    }

    @Override
    public void createJournal(InvestmentJournalDto dto) {
        investmentJournalMapper.insertJournal(dto.toVo());
    }

    @Override
    public List<InvestmentJournalDto> getJournalsByUserId(int userId) {
        return investmentJournalMapper.findByUserId(userId).stream()
                .map(InvestmentJournalDto::of)
                .collect(Collectors.toList());
    }

    @Override
    public InvestmentJournalDto getJournalById(int id) {
        InvestmentJournalVO vo = investmentJournalMapper.findById(id);
        return InvestmentJournalDto.of(vo);
    }

    @Override
    public void updateJournal(InvestmentJournalDto dto) {
        investmentJournalMapper.updateJournal(dto.toVo());
    }

    @Override
    public void deleteJournal(int id) {
        investmentJournalMapper.deleteJournal(id);
    }
}
