package org.scoula.service.learning;

import org.scoula.domain.learning.dto.LearningContentDTO;

import java.util.List;

public interface LearningGptService {

    List<LearningContentDTO> recommendLearningContents(Long userId, int size);
}
