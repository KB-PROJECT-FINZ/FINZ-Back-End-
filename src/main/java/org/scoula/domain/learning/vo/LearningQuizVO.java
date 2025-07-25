package org.scoula.domain.learning.vo;

import lombok.Data;

@Data
public class LearningQuizVO {
    private int quizId;
    private String question;
    private String answer;
    private String comment;
    private int creditReward;
}
