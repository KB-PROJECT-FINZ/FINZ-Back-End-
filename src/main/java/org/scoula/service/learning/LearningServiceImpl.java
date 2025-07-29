package org.scoula.service.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.dto.LearningHistoryDto;
import org.scoula.domain.learning.dto.LearningQuizDTO;

import org.scoula.domain.learning.vo.LearningQuizVO;
import org.scoula.mapper.LearningMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final LearningMapper learningMapper;

    @Override
    public List<LearningContentDTO> getAllContents() {
        return learningMapper.getAllContents()
                .stream()
                .map(vo -> new LearningContentDTO(vo))
                .collect(Collectors.toList());
    }

    @Override
    public LearningQuizDTO getQuizByContentId(int contentId) {
        LearningQuizVO vo = learningMapper.getQuizByContentId(contentId);
        return new LearningQuizDTO(vo);
    }

    @Override
    public List<LearningContentDTO> getContentsByGroupCode(String groupCode) {
        return learningMapper.getContentsByGroupCode(groupCode)
                .stream()
                .map(LearningContentDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public LearningContentDTO getContentById(int id) {
        return learningMapper.getContentById(id);
    }

    @Override
    public void saveLearningHistory(LearningHistoryDto dto) {
        learningMapper.insertLearningHistory(dto);
    }

    @Override
    public int giveCredit(int userId, int quizId) {
        // 퀴즈 정보 조회
        LearningQuizDTO quiz = getQuizByContentId(quizId);
        int creditAmount = quiz.getCreditReward();
        
        // 사용자 크레딧 업데이트
        learningMapper.updateUserCredit(userId, creditAmount);
        
        return creditAmount;
    }

    @Override
    public boolean hasCompleted(int userId, int contentId){
        return learningMapper.isUserIdAndContentId(userId,contentId)>0;
    }

    @Override
    public List<LearningHistoryDto> getLearningHistoryList(int userId) {
        return learningMapper.getLearningHistoryList(userId)
                .stream()
                .map(vo -> new LearningHistoryDto(vo))
                .collect(Collectors.toList());
    }

    @Override
    public int getUserCredit(int userId) {
        return learningMapper.getUserCredit(userId);
    }

    @Override
    public boolean checkQuiz(int userId, int quizId) {
        return learningMapper.hasQuizResult(userId, quizId);
    }

    @Override
    public void saveResult(int userId, int quizId, boolean isCorrect, String selectedAnswer, int creditEarned) {
        learningMapper.saveQuizResult(userId, quizId, isCorrect, selectedAnswer, creditEarned);
    }
}
