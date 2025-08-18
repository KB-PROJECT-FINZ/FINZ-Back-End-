package org.scoula.controller.learning;

import org.scoula.domain.learning.dto.ContentDto;
import org.scoula.service.learning.LearningContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/all-recommend")
    public List<ContentDto> getAllContents() {
        return learningContentService.getAllContents();
    }
    @GetMapping("/detail/{id}")
    public ResponseEntity<ContentDto> getDetail(@PathVariable Long id) {
        ContentDto content = learningContentService.getContentById(id); // 👈 여기에 값이 null이면 모달도 빈다
        return ResponseEntity.ok(content);
    }

}

