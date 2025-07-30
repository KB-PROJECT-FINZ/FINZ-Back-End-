package org.scoula.controller.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.dto.LearningHistoryDto;
import org.scoula.domain.learning.dto.LearningQuizDTO;
import org.scoula.domain.learning.dto.QuizResultDTO;
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

    // 사용자 크레딧 조회
    @GetMapping("/user/credit/{userId}")
    public ResponseEntity<Integer> getUserCredit(@PathVariable int userId) {
        try {
            int totalCredit = learningService.getUserCredit(userId);
            return ResponseEntity.ok(totalCredit);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(0);
        }
    }

    // 퀴즈 정답 시 크레딧 지급
    @PostMapping("/quiz/credit")
    public ResponseEntity<String> giveCredit(@RequestParam int userId, @RequestParam int quizId, 
                                           @RequestParam String selectedAnswer) {
        try {
            // 이미 퀴즈를 풀었는지 확인
            if (learningService.checkQuiz(userId, quizId)) {
                return ResponseEntity.badRequest().body("이미 퀴즈를 푸신 콘텐츠입니다.");
            }
            
            // 정답 여부 확인
            LearningQuizDTO quiz = learningService.getQuizByContentId(quizId);
            boolean isCorrect = selectedAnswer.equals(quiz.getAnswer());
            
            int creditAmount = 0;
            if (isCorrect) {
                creditAmount = learningService.giveCredit(userId, quizId);
            } else {
                // 오답인 경우에도 결과 저장 (크레딧은 0)
                learningService.saveResult(userId, quizId, false, selectedAnswer, 0);
            }
            
            return ResponseEntity.ok("퀴즈 결과가 저장되었습니다." + (isCorrect ? " 크레딧 " + creditAmount + "개 지급 완료" : ""));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("퀴즈 처리 실패");
        }
    }

    // 퀴즈 결과 확인
    @GetMapping("/quiz/result")
    public ResponseEntity<Boolean> checkQuiz(@RequestParam int userId, @RequestParam int quizId) {
        try {
            boolean hasResult = learningService.checkQuiz(userId, quizId);
            return ResponseEntity.ok(hasResult);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(false);
        }
    }

    // 퀴즈 결과 상세 조회
    @GetMapping("/quiz/result/detail")
    public ResponseEntity<QuizResultDTO> getQuizResult(@RequestParam int userId, @RequestParam int quizId) {
        try {
            QuizResultDTO result = learningService.getQuizResult(userId, quizId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // 퀴즈 결과 저장 (오답용)
    @PostMapping("/quiz/result/save")
    public ResponseEntity<String> saveQuizResult(@RequestBody QuizResultDTO dto) {
        try {
            learningService.saveResult(dto.getUserId(), dto.getQuizId(), dto.isCorrect(), dto.getSelectedAnswer(), 0);
            return ResponseEntity.ok("퀴즈 결과가 저장되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("퀴즈 결과 저장 실패");
        }
    }

    //컨텐츠 상세페이지에서 읽었는지 유무 판단하기 위해
    @GetMapping("/history/complete")
    public boolean userCompletedContent(@RequestParam int userId, @RequestParam int contentId) {
        return learningService.hasCompleted(userId, contentId);
    }

    //컨텐츠 리스트 중 읽은 글은 회색 처리 하기 위해서
    @GetMapping("/history/complete/list")
    public ResponseEntity<List<LearningHistoryDto>> getLearningHistoryList(@RequestParam int userId) {
        return ResponseEntity.ok(learningService.getLearningHistoryList(userId));
    }
}

