package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.dto.LearningHistoryDto;
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
    int isUserIdAndContentId(@Param("userId") int userId, @Param("contentId") int contentId);
    List<LearningHistoryVO> getLearningHistoryList(@Param("userId") int userId);
    String findGroupCodeByUserId(Long userId);
    List<LearningContentVO> findUnreadContent(
            @Param("groupCode") String groupCode,
            @Param("userId") Long userId
    );
    List<String> findTitlesByGroupCode(String groupCode);
    void insertContent(LearningContentVO content);
    List<LearningContentVO> findCompletedContentByUserId(Long userId);
    void insertQuiz(LearningQuizVO quiz);

}
