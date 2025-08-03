package org.scoula.controller.learning;

import org.scoula.domain.learning.dto.ContentDto;
import org.scoula.service.learning.LearningContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contents")
public class LearningContentController {

    @Autowired
    private LearningContentService learningContentService;

    @GetMapping("/recommend")
    public ResponseEntity<List<ContentDto>> getRecommendedByRiskType(@RequestParam String riskType) {
        List<ContentDto> contents = learningContentService.getContentsByRiskType(riskType);
        return ResponseEntity.ok(contents);
    }
}

