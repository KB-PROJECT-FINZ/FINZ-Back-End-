package org.scoula.service.feedback;

import org.scoula.domain.feedback.dto.FeedbackSaveDto;

import java.sql.Date;

public interface JournalFeedbackService {
    void saveFeedback(FeedbackSaveDto dto);
}
