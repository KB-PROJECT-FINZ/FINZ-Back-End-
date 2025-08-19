package org.scoula.service.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.vo.LearningContentVO;
import org.scoula.domain.learning.vo.LearningQuizVO;
import org.scoula.mapper.learning.LearningMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningSaveService {
    private final LearningMapper learningMapper;

    @Transactional
    public void saveContentAndQuiz(LearningContentVO content, LearningQuizVO quiz){
        learningMapper.insertContent(content);
        quiz.setQuizId(content.getContentId());
        learningMapper.insertQuiz(quiz);
    }
}
