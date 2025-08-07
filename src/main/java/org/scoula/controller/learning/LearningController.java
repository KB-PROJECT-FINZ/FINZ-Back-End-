package org.scoula.controller.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.config.auth.LoginUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.dto.LearningHistoryDto;
import org.scoula.domain.learning.dto.LearningQuizDTO;
import org.scoula.domain.learning.dto.QuizResultDTO;
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
    public ResponseEntity<String> insertLearningHistory(@LoginUser UserVo user, @RequestBody LearningHistoryDto dto) {
        try {
            dto.setUserId(user.getId());
            learningService.saveLearningHistory(dto);
            return ResponseEntity.ok("학습 기록 저장 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("학습 기록 저장 실패");
        }
    }

    // 사용자 크레딧 조회
    @GetMapping("/user/credit")
    public ResponseEntity<Integer> getUserCredit(@LoginUser UserVo user) {
        try {
            int totalCredit = learningService.getUserCredit(user.getId());
            return ResponseEntity.ok(totalCredit);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(0);
        }
    }

    // 퀴즈 정답 시 크레딧 지급
    @PostMapping("/quiz/credit")
    public ResponseEntity<String> giveCredit(@LoginUser UserVo user, @RequestParam int quizId,
                                             @RequestParam String selectedAnswer) {
        try {
            // 이미 퀴즈를 풀었는지 확인
            if (learningService.checkQuiz(user.getId(), quizId)) {
                return ResponseEntity.badRequest().body("이미 퀴즈를 푸신 콘텐츠입니다.");
            }

            // 정답 여부 확인
            LearningQuizDTO quiz = learningService.getQuizByContentId(quizId);
            boolean isCorrect = selectedAnswer.equals(quiz.getAnswer());

            int creditAmount = 0;
            if (isCorrect) {
                creditAmount = learningService.giveCredit(user.getId(), quizId);
            } else {
                // 오답인 경우에도 결과 저장 (크레딧은 0)
                learningService.saveResult(user.getId(), quizId, false, selectedAnswer, 0);
            }

            return ResponseEntity.ok("퀴즈 결과가 저장되었습니다." + (isCorrect ? " 크레딧 " + creditAmount + "개 지급 완료" : ""));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("퀴즈 처리 실패");
        }
    }

    // 퀴즈 결과 확인
    @GetMapping("/quiz/result")
    public ResponseEntity<Boolean> checkQuiz(@LoginUser UserVo user, @RequestParam int quizId) {
        try {
            boolean hasResult = learningService.checkQuiz(user.getId(), quizId);
            return ResponseEntity.ok(hasResult);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(false);
        }
    }

    // 퀴즈 결과 상세 조회
    @GetMapping("/quiz/result/detail")
    public ResponseEntity<QuizResultDTO> getQuizResult(@LoginUser UserVo user, @RequestParam int quizId) {
        try {
            QuizResultDTO result = learningService.getQuizResult(user.getId(), quizId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // 퀴즈 결과 저장 (오답용)
    @PostMapping("/quiz/result/save")
    public ResponseEntity<String> saveQuizResult(@LoginUser UserVo user, @RequestBody QuizResultDTO dto) {
        try {
            learningService.saveResult(user.getId(), dto.getQuizId(), dto.isCorrect(), dto.getSelectedAnswer(), 0);
            return ResponseEntity.ok("퀴즈 결과가 저장되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("퀴즈 결과 저장 실패");
        }
    }

    //컨텐츠 상세페이지에서 읽었는지 유무 판단하기 위해
    @GetMapping("/history/complete")
    public boolean userCompletedContent(@LoginUser UserVo user, @RequestParam int contentId) {
        return learningService.hasCompleted(user.getId(), contentId);
    }

    //완료된 학습 컨텐츠 분류
    @GetMapping("/history/complete/list")
    public List<LearningContentDTO> getCompletedContents(@LoginUser UserVo user) {
        return learningService.getCompletedContents(user.getId());
    }

    //    사용자 별 추천 콘텐츠 분류
    @GetMapping("/recommend/list")
    public ResponseEntity<List<LearningContentDTO>> recommendList(@LoginUser UserVo user) {
        return ResponseEntity.ok(learningGptService.recommendLearningContents(user.getId(), 5));
    }

    // 누적 획득 크레딧 조회
    @GetMapping("/user/total-earned-credit")
    public ResponseEntity<Integer> getTotalEarnedCredit(@LoginUser UserVo user) {
        try {
            int totalEarnedCredit = learningService.getTotalEarnedCredit(user.getId());
            return ResponseEntity.ok(totalEarnedCredit);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(0);
        }
    }

    //사용자 별 완료된 컨텐츠 개수 조회
    @GetMapping("/history/count")
    public int getUserReadCount(@LoginUser UserVo user) {
        return learningService.getUserReadCount(user.getId());
    }

}



