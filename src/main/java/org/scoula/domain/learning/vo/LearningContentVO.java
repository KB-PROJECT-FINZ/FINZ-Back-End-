package org.scoula.domain.learning.vo;

import lombok.Data;

@Data
public class LearningContentVO {
    private int contentId;
    private String type;
    private String title;
    private String body;
    private String imageUrl;
    private String youtubeUrl;
    private String createdAt;
}
