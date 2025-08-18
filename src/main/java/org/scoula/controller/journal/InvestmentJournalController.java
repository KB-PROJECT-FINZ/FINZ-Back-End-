package org.scoula.controller.journal;

import lombok.RequiredArgsConstructor;
import org.scoula.config.auth.LoginUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.journal.dto.InvestmentJournalDto;
import org.scoula.service.journal.InvestmentJournalService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class InvestmentJournalController {

    private final InvestmentJournalService journalService;

    @PostMapping
    public void createJournal(@RequestBody InvestmentJournalDto dto,
                              @LoginUser UserVo user) {
        dto.setUserId(user.getId()); // 세션에서 로그인된 유저 ID 주입
        journalService.createJournal(dto);
    }

    @GetMapping("/user")
    public List<InvestmentJournalDto> getMyJournals(@LoginUser UserVo user) {
        return journalService.getJournalsByUserId(user.getId());
    }

    @GetMapping("/{id}")
    public InvestmentJournalDto getJournalById(@PathVariable int id) {
        return journalService.getJournalById(id);
    }

    @PutMapping("/{id}")
    public void updateJournal(@PathVariable int id, @RequestBody InvestmentJournalDto dto,@LoginUser UserVo user) {
        dto.setId(id);
        dto.setUserId(user.getId()); // 로그인한 사용자 ID
        journalService.updateJournal(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteJournal(@PathVariable int id) {
        journalService.deleteJournal(id);
    }
}
