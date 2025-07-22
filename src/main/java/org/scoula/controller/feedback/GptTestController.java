package org.scoula.controller.feedback;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.feedback.dto.GptRequestDto;
import org.scoula.domain.journal.vo.InvestmentJournalVO;
import org.scoula.mapper.JournalFeedbackMapper;
import org.scoula.service.feedback.GptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class GptTestController {

    private final GptService gptService;
    private final JournalFeedbackMapper journalFeedbackMapper;

    @GetMapping(value = "/gpt", produces = "text/plain; charset=UTF-8")
    public String callGpt() {
        int userId = 1;

        // 이번 주 월요일 ~ 금요일 날짜 계산
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate friday = monday.plusDays(4);

        // DB에서 해당 주간 일지 가져오기
        List<InvestmentJournalVO> journals = journalFeedbackMapper.getJournalsByUserAndDateRange(
                userId,
                Date.valueOf(monday),
                Date.valueOf(friday)
        );

        // GPT 호출 및 결과 반환
        GptRequestDto requestDto = new GptRequestDto(journals);
        return gptService.callGpt(requestDto);
    }
}
