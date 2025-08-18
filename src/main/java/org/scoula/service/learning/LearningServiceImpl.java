package org.scoula.service.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.*;

import org.scoula.domain.learning.vo.LearningQuizVO;
import org.scoula.mapper.learning.LearningMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void giveCreditOnce(int userId){
        learningMapper.updateUserCredit(userId,8000);
    }
    @Override
    @Transactional
    public int giveCredit(int userId, int quizId) {
        try {
            // 퀴즈 정보 조회
            LearningQuizDTO quiz = getQuizByContentId(quizId);
            int creditAmount = quiz.getCreditReward();

            // 사용자 크레딧 업데이트
            int updatedRows = learningMapper.updateUserCredit(userId, creditAmount);

            if (updatedRows == 0) {
                throw new RuntimeException("사용자 크레딧 업데이트 실패: 사용자를 찾을 수 없습니다.");
            }

            // 퀴즈 결과 저장 (정답으로 처리)
            learningMapper.saveQuizResult(userId, quizId, true, "O", creditAmount);
            // 누적 획득 크레딧 업데이트
            learningMapper.updateTotalEarnedCredit(userId, creditAmount);

            return creditAmount;
        } catch (Exception e) {
            throw new RuntimeException("크레딧 지급 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasCompleted(int userId, int contentId){
        return learningMapper.isUserIdAndContentId(userId,contentId)>0;
    }

    public List<LearningContentDTO> getCompletedContents(int userId) {
        return learningMapper.findCompletedContentByUserId(userId)
                .stream()
                .map(LearningContentDTO::new)
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

    @Override
    public QuizResultDTO getQuizResult(int userId, int quizId) {
        return learningMapper.getQuizResult(userId, quizId);
    }

    @Override
    public int getTotalEarnedCredit(int userId) {
        return learningMapper.getTotalEarnedCredit(userId);
    }
  
    @Override
    public int getUserReadCount(int userId) {
        return learningMapper.getUserReadCount(userId);

    }

}
