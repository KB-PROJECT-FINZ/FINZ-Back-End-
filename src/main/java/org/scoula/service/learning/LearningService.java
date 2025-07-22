package org.scoula.service.learning;

import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.dto.LearningQuizDTO;
import org.scoula.domain.learning.dto.LearningQuizSubmitDTO;
import org.scoula.domain.learning.vo.LearningContentVO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LearningService {
    List<LearningContentDTO> getAllContents();
    List<LearningContentDTO> getContentsByGroupCode(String groupCode);
    LearningQuizDTO getQuizByContentId(int contentId);
    LearningContentDTO getContentById(int id);
}
