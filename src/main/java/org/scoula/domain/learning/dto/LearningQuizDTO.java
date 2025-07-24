package org.scoula.domain.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.learning.vo.LearningQuizVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningQuizDTO {
    private int quizId;
    private String question;
    private String answer;
    private String comment;
    private int creditReward;

    public LearningQuizDTO(LearningQuizVO vo) {
        this.quizId = vo.getQuizId();
        this.question = vo.getQuestion();
        this.answer = vo.getAnswer();
        this.comment = vo.getComment();
        this.creditReward = vo.getCreditReward();
    }
}
