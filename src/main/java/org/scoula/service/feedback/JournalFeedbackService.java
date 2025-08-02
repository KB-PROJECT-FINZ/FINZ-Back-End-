package org.scoula.service.feedback;

import org.scoula.domain.feedback.dto.FeedbackSaveDto;
import org.scoula.domain.feedback.vo.JournalFeedback;

import java.sql.Date;
import java.util.List;

public interface JournalFeedbackService {
    void saveFeedback(FeedbackSaveDto dto);
    List<JournalFeedback> getAllFeedbacks(int userId);

}
