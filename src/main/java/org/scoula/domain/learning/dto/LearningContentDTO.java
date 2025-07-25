package org.scoula.domain.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.domain.learning.vo.LearningContentVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningContentDTO {
    private int contentId;
    private String type;
    private String title;
    private String body;
    private String imageUrl;
    private String youtubeUrl;
    private String createdAt;

    public LearningContentDTO(LearningContentVO vo) {
        this.contentId = vo.getContentId();
        this.type = vo.getType();
        this.title = vo.getTitle();
        this.body = vo.getBody();
        this.imageUrl = vo.getImageUrl();
        this.youtubeUrl = vo.getYoutubeUrl();
        this.createdAt = vo.getCreatedAt();
    }
}
