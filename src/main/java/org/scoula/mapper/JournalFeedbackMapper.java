package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.journal.vo.InvestmentJournalVO;
import org.scoula.domain.feedback.vo.JournalFeedback;
import java.util.Date;
import java.util.List;

@Mapper
public interface JournalFeedbackMapper {
    List<InvestmentJournalVO> getJournalsByUserAndDateRange(@Param("userId") int userId,
                                                            @Param("start") Date start,
                                                            @Param("end") Date end);

    void insertFeedback(JournalFeedback feedback);
    void insertFeedbackMapping(@Param("journalId") int journalId, @Param("feedbackId") int feedbackId);
    JournalFeedback findFeedbackByUserAndWeek(@Param("userId") int userId,
                                              @Param("start") Date start,
                                              @Param("end") Date end);
    List<JournalFeedback> findAllFeedbacks();
}
