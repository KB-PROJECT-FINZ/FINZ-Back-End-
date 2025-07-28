package org.scoula.controller.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.dto.LearningHistoryDto;
import org.scoula.domain.learning.dto.LearningQuizDTO;
import org.scoula.mapper.LearningMapper;
import org.scoula.service.learning.LearningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;
    //콘텐츠 목록
    @GetMapping("/contents")
    public ResponseEntity<List<LearningContentDTO>> getAllContents() {
        return ResponseEntity.ok(learningService.getAllContents());
    }

    //퀴즈 조회
    @GetMapping("/{id}/quiz")
    public ResponseEntity<LearningQuizDTO> getQuiz(@PathVariable int id) {
        return ResponseEntity.ok(learningService.getQuizByContentId(id));
    }

    //사용자 투자 성향 기반 콘텐츠 추천
    @GetMapping("/contents/by-group")
    public ResponseEntity<List<LearningContentDTO>> getContentsByGroupCode(@RequestParam String groupCode) {
        return ResponseEntity.ok(learningService.getContentsByGroupCode(groupCode));
    }

    // 콘텐츠 상세 조회
    @GetMapping("/contents/{id}")
    public ResponseEntity<LearningContentDTO> getContentById(@PathVariable int id) {
        return ResponseEntity.ok(learningService.getContentById(id));
    }
    @PostMapping("/history")
    public ResponseEntity<String> insertLearningHistory(@RequestBody LearningHistoryDto dto) {
        try {
            learningService.saveLearningHistory(dto);
            return ResponseEntity.ok("학습 기록 저장 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("학습 기록 저장 실패");
        }
    }

    // 퀴즈 정답 시 크레딧 지급
    @PostMapping("/quiz/credit")
    public ResponseEntity<String> awardQuizCredit(@RequestParam int userId, @RequestParam int quizId) {
        try {
            int creditAmount = learningService.awardQuizCredit(userId, quizId);
            return ResponseEntity.ok("크레딧 " + creditAmount + "개 지급 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("크레딧 지급 실패");
        }
    }
}
