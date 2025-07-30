package org.scoula.domain.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDTO {
    private int userId;
    private int quizId;
    private boolean isCorrect;
    private String selectedAnswer;
    private int creditEarned;
    private LocalDateTime attemptedAt;
}