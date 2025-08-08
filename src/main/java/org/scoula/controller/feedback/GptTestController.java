package org.scoula.controller.feedback;

import lombok.RequiredArgsConstructor;
import org.scoula.config.auth.LoginUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.feedback.dto.FeedbackResponseDto;
import org.scoula.domain.feedback.dto.GptRequestDto;
import org.scoula.domain.feedback.vo.JournalFeedback;
import org.scoula.domain.journal.vo.InvestmentJournalVO;
import org.scoula.mapper.JournalFeedbackMapper;
import org.scoula.service.feedback.GptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class GptTestController {

    private final GptService gptService;
    private final JournalFeedbackMapper journalFeedbackMapper;@GetMapping("/gpt")
    public FeedbackResponseDto callGpt(HttpSession session, @LoginUser UserVo user) {
        int userId = user.getId();

        LocalDate today = LocalDate.now();
        LocalDate thisMon = today.with(DayOfWeek.MONDAY);
        LocalDate thisFri = thisMon.plusDays(4);

        // 지난주 월~일
        LocalDate lastMon = thisMon.minusWeeks(1);
        LocalDate lastFri = thisFri.minusWeeks(1);

        boolean isWeekday = today.getDayOfWeek().getValue() <= 5; // 1=월 ... 5=금

        Date start, end;

        if (isWeekday) {
            // 월~금: 지난주 주간 피드백
            start = Date.valueOf(lastMon);
            end   = Date.valueOf(lastFri);
        } else {
            // 토/일: 이번주 주간 피드백
            start = Date.valueOf(thisMon);
            end   = Date.valueOf(thisFri);
        }

        // 1) 기존 피드백 조회
        JournalFeedback saved = journalFeedbackMapper.findFeedbackByUserAndWeek(userId, start, end);
        if (saved != null) {
            return new FeedbackResponseDto(saved.getUserId(), saved.getWeekStart(), saved.getWeekEnd(), saved.getFeedback(), null);
        }

        // 2) 일지 조회
        List<InvestmentJournalVO> journals = journalFeedbackMapper.getJournalsByUserAndDateRange(userId, start, end);
        if (journals == null || journals.isEmpty()) {
            return new FeedbackResponseDto(
                    userId, start, end, null,
                    (isWeekday ? "지난주(월~금)" : "이번 주(월~금)") + " 작성된 일지가 없어 피드백을 생성할 수 없습니다."
            );
        }

        // 3) 생성 & 저장
        String feedback = gptService.callGpt(new GptRequestDto(journals));
        JournalFeedback newFeedback = JournalFeedback.builder()
                .userId(userId).weekStart(start).weekEnd(end).feedback(feedback).build();
        journalFeedbackMapper.insertFeedback(newFeedback);

        return new FeedbackResponseDto(newFeedback.getUserId(), newFeedback.getWeekStart(), newFeedback.getWeekEnd(), newFeedback.getFeedback(), null);
    }

}
