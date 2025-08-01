package org.scoula.service.learning;

import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.dto.LearningHistoryDto;
import org.scoula.domain.learning.dto.LearningQuizDTO;
import org.scoula.domain.learning.dto.QuizResultDTO;
import java.util.List;

public interface LearningService {
    List<LearningContentDTO> getAllContents();
    List<LearningContentDTO> getContentsByGroupCode(String groupCode);
    LearningQuizDTO getQuizByContentId(int contentId);
    LearningContentDTO getContentById(int id);
    void saveLearningHistory(LearningHistoryDto dto);
    int giveCredit(int userId, int quizId);
    int getUserCredit(int userId);
    boolean checkQuiz(int userId, int quizId);
    boolean hasCompleted(int userId, int contentId);
    void saveResult(int userId, int quizId, boolean isCorrect, String selectedAnswer, int creditEarned);
    QuizResultDTO getQuizResult(int userId, int quizId);
    List<LearningContentDTO> getCompletedContents(int userId);
    int getTotalEarnedCredit(int userId);
}
