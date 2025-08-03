package org.scoula.domain.learning.dto;

import lombok.Data;

@Data
public class ContentDto {
    private Long id;
    private String label;
    private String riskType;
    private String title;
    private String content;
}
