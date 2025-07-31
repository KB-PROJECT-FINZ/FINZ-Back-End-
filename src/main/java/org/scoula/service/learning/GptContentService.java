package org.scoula.service.learning;

import org.scoula.domain.learning.dto.GptLearningContentResponseDto;

import java.util.List;

public interface GptContentService {
    GptLearningContentResponseDto generateContent(String groupCode, List<String> existingTitles);
}
