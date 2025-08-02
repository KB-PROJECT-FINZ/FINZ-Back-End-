package org.scoula.service.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.GptLearningContentResponseDto;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.vo.LearningContentVO;
import org.scoula.domain.learning.vo.LearningQuizVO;
import org.scoula.mapper.LearningMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningGptServiceImpl implements LearningGptService {

    private final LearningMapper learningMapper;
    private final LearningGptAsyncHelper learningGptAsyncHelper;
    @Override
    public List<LearningContentDTO> recommendLearningContents(int userId, int size) {
        String groupCode = learningMapper.findGroupCodeByUserId(userId);
        List<LearningContentVO> unreadContents = learningMapper.findUnreadContent(groupCode, userId);
        int remainToGenerate = size - unreadContents.size();

        if (remainToGenerate > 0) {
            learningGptAsyncHelper.asyncGenerateAndSaveContents(groupCode, remainToGenerate);
        }

        return unreadContents.stream()
                .limit(size)
                .map(LearningContentDTO::new)
                .toList();
    }
}
