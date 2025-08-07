package org.scoula.controller.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.config.auth.LoginUser;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.feedback.dto.AIAnalysisReportDto;
import org.scoula.domain.feedback.vo.JournalFeedback;
import org.scoula.service.feedback.AIAnalysisReportService;
import org.scoula.service.feedback.JournalFeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedback")
@Log4j2
public class FeedbackListController {
    private final JournalFeedbackService journalFeedbackService;
    private final AIAnalysisReportService aiAnalysisReportService;

    @GetMapping
    public List<JournalFeedback> getAllFeedbacks(@LoginUser UserVo user) {
        return journalFeedbackService.getAllFeedbacks(user.getId());
    }

    /**
     * 현재 로그인한 사용자의 최신 AI 분석 리포트 조회
     * GET /api/feedback/behavior
     */
    @GetMapping("/behavior")
    public ResponseEntity<?> getLatestAnalysisReport(@LoginUser UserVo user) {
        try {
            if (user == null) {
                log.warn("로그인되지 않은 사용자의 AI 분석 리포트 접근 시도");
                return ResponseEntity.status(401).body(Map.of(
                        "message", "로그인이 필요합니다.",
                        "data", null
                ));
            }

            Integer userId = user.getId();
            log.info("사용자 {}의 최신 AI 분석 리포트 조회 요청", userId);

            // AI 분석 리포트 서비스를 통해 최신 리포트 조회
            AIAnalysisReportDto latestReport = aiAnalysisReportService.getLatestByUserId(userId);

            if (latestReport == null) {
                log.info("사용자 {}의 AI 분석 리포트가 존재하지 않음", userId);
                return ResponseEntity.ok(Map.of(
                        "message", "분석 결과가 없습니다. 충분한 거래 데이터가 쌓인 후 AI 분석을 요청해주세요.",
                        "data", null
                ));
            }

            // DTO를 프론트엔드 응답 형태로 변환
            Map<String, Object> responseData = Map.of(
                    "stats", Map.of(
                            "transactionCount", latestReport.getTransactionCount(),
                            "analysisPeriod", latestReport.getAnalysisPeriodDays(),
                            "startDate", latestReport.getStartDate().toString(),
                            "endDate", latestReport.getEndDate().toString(),
                            "totalReturn", 0.0 // TODO: 수익률 계산 로직 추가 필요
                    ),
                    "aiAnalysis", Map.of(
                            "strategy", latestReport.getSummaryText(),
                            "risks", latestReport.getRiskText() != null ? latestReport.getRiskText() : "",
                            "advice", latestReport.getSuggestionText() != null ? latestReport.getSuggestionText() : ""
                    )
            );

            log.info("사용자 {}의 AI 분석 리포트 조회 성공 - 리포트 ID: {}", userId, latestReport.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "분석 결과 조회 성공",
                    "data", responseData
            ));

        } catch (Exception e) {
            log.error("AI 분석 리포트 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "분석 결과 조회 중 오류가 발생했습니다.",
                    "error", e.getMessage()
            ));
        }
    }
}