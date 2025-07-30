package org.scoula.controller.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.dto.LearningHistoryDto;
import org.scoula.domain.learning.dto.LearningQuizDTO;
import org.scoula.mapper.LearningMapper;
import org.scoula.service.learning.LearningGptService;
import org.scoula.service.learning.LearningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;

    private final LearningGptService learningGptService;

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
    //컨텐츠 상세페이지에서 읽었는지 유무 판단하기 위해
    @GetMapping("/history/complete")
    public boolean userCompletedContent(@RequestParam int userId, @RequestParam int contentId) {
        return learningService.hasCompleted(userId, contentId);
    }

    //컨텐츠 리스트 중 읽은 글은 회색 처리 하기 위해서
//    @GetMapping("/history/complete/list")
//    public ResponseEntity<List<LearningHistoryDto>> getLearningHistoryList(@RequestParam int userId) {
//        return ResponseEntity.ok(learningService.getLearningHistoryList(userId));
//    }
    // LearningController
    @GetMapping("/history/complete/list")
    public List<LearningContentDTO> getCompletedContents(@RequestParam Long userId) {
        return learningService.getCompletedContents(userId);
    }

//    @GetMapping("/recommend/list")
//    public ResponseEntity<List<LearningContentDTO>> recommendList(
//            @RequestParam("userId") Long userId,
//            @RequestParam(name = "size", defaultValue = "5") int size
//    ) {
//        List<LearningContentDTO> contents = learningGptService.recommendLearningContents(userId, size);
//        return ResponseEntity.ok(contents);
//    }

    @GetMapping("/recommend/list")
    public ResponseEntity<List<LearningContentDTO>> recommendList(
            @RequestParam Long userId,
            HttpSession session
    ) {
        System.out.println("현재 세션 ID: " + session.getId());
        System.out.println("요청된 userId: " + userId);
        return ResponseEntity.ok(learningGptService.recommendLearningContents(userId, 5));
    }

}
