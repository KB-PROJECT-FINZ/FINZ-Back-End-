package org.scoula.domain.learning.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.learning.vo.LearningHistoryVO;

@Data
@NoArgsConstructor
public class LearningHistoryDto {
    private int userId;
    private int contentId;

    public LearningHistoryDto(LearningHistoryVO vo) {
        this.userId = vo.getUserId();
        this.contentId = vo.getContentId();
    }
}
