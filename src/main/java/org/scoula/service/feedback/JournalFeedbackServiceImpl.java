package org.scoula.service.feedback;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.feedback.dto.FeedbackSaveDto;
import org.scoula.domain.feedback.vo.JournalFeedback;
import org.scoula.mapper.JournalFeedbackMapper;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalFeedbackServiceImpl implements JournalFeedbackService {

    private final JournalFeedbackMapper journalFeedbackMapper;

    @Override
    public void saveFeedback(FeedbackSaveDto dto) {
        JournalFeedback feedBack = JournalFeedback.builder()
                .userId(dto.getUserId())
                .weekStart(dto.getWeekStart())
                .weekEnd(dto.getWeekEnd())
                .feedback(dto.getFeedback())
                .build();

        journalFeedbackMapper.insertFeedback(feedBack);
    }
    @Override
    public List<JournalFeedback> getAllFeedbacks() {
        return journalFeedbackMapper.findAllFeedbacks();
    }
}
