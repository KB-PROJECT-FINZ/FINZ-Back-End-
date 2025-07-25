package org.scoula.domain.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LearningQuizSubmitDTO {
    private int quizId;
    private String answer;
    private Long userId; // 유저 정보 필요
}
