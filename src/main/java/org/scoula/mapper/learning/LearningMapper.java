package org.scoula.mapper.learning;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.learning.dto.*;
import org.scoula.domain.learning.dto.QuizResultDTO;
import org.scoula.domain.learning.vo.LearningContentVO;
import org.scoula.domain.learning.vo.LearningHistoryVO;
import org.scoula.domain.learning.vo.LearningQuizVO;

import java.util.List;

@Mapper
public interface LearningMapper {
    List<LearningContentVO> getAllContents();
    List<LearningContentVO> getContentsByGroupCode(String groupCode);
    LearningQuizVO getQuizByContentId(int contentId);
    LearningQuizVO getQuizById(int quizId);
    LearningContentDTO getContentById(int id);
    void insertLearningHistory(LearningHistoryDto dto);
    int updateUserCredit(@Param("userId") int userId, @Param("creditAmount") int creditAmount);
    int getUserCredit(int userId);
    boolean hasQuizResult(@Param("userId") int userId, @Param("quizId") int quizId);
    void saveQuizResult(@Param("userId") int userId, @Param("quizId") int quizId,
                        @Param("isCorrect") boolean isCorrect, @Param("selectedAnswer") String selectedAnswer,
                        @Param("creditEarned") int creditEarned);
    int isUserIdAndContentId(@Param("userId") int userId, @Param("contentId") int contentId);
    List<LearningHistoryVO> getLearningHistoryList(@Param("userId") int userId);
    String findGroupCodeByUserId(@Param("userId") int userId);
    List<LearningContentVO> findUnreadContent(
            @Param("groupCode") String groupCode,
            @Param("userId") int userId
    );
    List<String> findTitlesByGroupCode(String groupCode);
    void insertContent(LearningContentVO content);
    List<LearningContentVO> findCompletedContentByUserId(@Param("userId") int userId);
    void insertQuiz(LearningQuizVO quiz);
    QuizResultDTO getQuizResult(@Param("userId") int userId, @Param("quizId") int quizId);
    int updateTotalEarnedCredit(@Param("userId") int userId, @Param("creditAmount") int creditAmount);
    int getTotalEarnedCredit(int userId);
    int getUserReadCount(@Param("userId") int userId);
    boolean existsQuizByContentId(int contentId);

}

