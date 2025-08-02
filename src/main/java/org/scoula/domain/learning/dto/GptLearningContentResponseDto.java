package org.scoula.domain.learning.dto;

import lombok.Data;

@Data
public class GptLearningContentResponseDto {
    private String title;
    private String body;

    private String quizQuestion;
    private String quizAnswer;
    private String quizComment;
    private int creditReward;
}
