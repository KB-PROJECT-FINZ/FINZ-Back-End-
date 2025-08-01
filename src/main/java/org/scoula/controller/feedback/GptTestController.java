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
import org.springframework.web.bind.annotation.RequestParam;
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
    private final JournalFeedbackMapper journalFeedbackMapper;
    @GetMapping("/gpt")
    public FeedbackResponseDto callGpt(HttpSession session, @LoginUser UserVo user) {

        int userId = user.getId();
        System.out.println(userId);
        // 이번 주 월요일 ~ 금요일 계산
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate friday = monday.plusDays(4);

        if (today.isBefore(friday.plusDays(1))) {
            return new FeedbackResponseDto(
                    userId,
                    Date.valueOf(monday),
                    Date.valueOf(friday),
                    null,
                    "이번 주 피드백은 월요일~금요일 데이터를 기반으로 제공됩니다. 아직 생성 중입니다."
            );
        }

        Date start = Date.valueOf(monday);
        Date end = Date.valueOf(friday);

        // DB에 이미 저장된 피드백 있는지 확인
        JournalFeedback feedback = journalFeedbackMapper.findFeedbackByUserAndWeek(userId, start, end);
        if (feedback != null) {
            return new FeedbackResponseDto(
                    feedback.getUserId(),
                    feedback.getWeekStart(),
                    feedback.getWeekEnd(),
                    feedback.getFeedback(),
                    null
            );
        }

        // 피드백 없으면 새로 생성
        List<InvestmentJournalVO> journals = journalFeedbackMapper.getJournalsByUserAndDateRange(userId, start, end);
        String generatedFeedback = gptService.callGpt(new GptRequestDto(journals));

        // DB에 저장
        JournalFeedback newFeedback = JournalFeedback.builder()
                .userId(userId)
                .weekStart(start)
                .weekEnd(end)
                .feedback(generatedFeedback)
                .build();

        journalFeedbackMapper.insertFeedback(newFeedback);

        return new FeedbackResponseDto(
                newFeedback.getUserId(),
                newFeedback.getWeekStart(),
                newFeedback.getWeekEnd(),
                newFeedback.getFeedback(),
                null
        );
    }
}
