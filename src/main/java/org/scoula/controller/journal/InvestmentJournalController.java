package org.scoula.controller.journal;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.journal.dto.InvestmentJournalDto;
import org.scoula.service.journal.InvestmentJournalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class InvestmentJournalController {

    private final InvestmentJournalService journalService;

    @PostMapping
    public void createJournal(@RequestBody InvestmentJournalDto dto) {
        journalService.createJournal(dto);
    }

    @GetMapping("/user/{userId}")
    public List<InvestmentJournalDto> getJournalsByUserId(@PathVariable int userId) {
        return journalService.getJournalsByUserId(userId);
    }

    @GetMapping("/{id}")
    public InvestmentJournalDto getJournalById(@PathVariable int id) {
        return journalService.getJournalById(id);
    }

    @PutMapping("/{id}")
    public void updateJournal(@PathVariable int id, @RequestBody InvestmentJournalDto dto) {
        dto.setId(id); // path param으로 받은 id를 dto에 세팅
        journalService.updateJournal(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteJournal(@PathVariable int id) {
        journalService.deleteJournal(id);
    }
}
