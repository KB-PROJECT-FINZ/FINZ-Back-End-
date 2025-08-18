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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * summary_text에서 수익률을 추출하는 메서드
     * %를 기준으로 역방향 탐색하여 숫자를 찾는 방식
     */
    private Double extractReturnRate(String summaryText) {
        if (summaryText == null || summaryText.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // % 기호를 찾아서 역방향으로 숫자 추출
            Pattern pattern = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)%");
            Matcher matcher = pattern.matcher(summaryText);

            Double lastFoundRate = null;

            // 모든 매칭되는 수익률을 찾아서 마지막 것을 사용 (보통 총 수익률이 마지막에 나옴)
            while (matcher.find()) {
                String returnRateStr = matcher.group(1);
                try {
                    lastFoundRate = Double.parseDouble(returnRateStr);
                    log.debug("발견된 수익률: {}%", lastFoundRate);
                } catch (NumberFormatException e) {
                    log.warn("수익률 파싱 실패: {}", returnRateStr);
                    continue;
                }
            }

            if (lastFoundRate != null) {
                log.info("추출된 수익률: {}%", lastFoundRate);
                return lastFoundRate;
            }

            log.warn("수익률 정보를 찾을 수 없습니다. 텍스트: {}", summaryText);
            return 0.0;

        } catch (Exception e) {
            log.error("수익률 추출 중 오류 발생: {}", e.getMessage());
            return 0.0;
        }
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

            // AI 분석 리포트 서비스를 통해 최신 리포트 조회
            AIAnalysisReportDto latestReport = aiAnalysisReportService.getLatestByUserId(userId);

            if (latestReport == null) {
                log.info("사용자 {}의 AI 분석 리포트가 존재하지 않음", userId);
                return ResponseEntity.ok(Map.of(
                        "message", "분석 결과가 없습니다. 충분한 거래 데이터가 쌓인 후 AI 분석을 요청해주세요.",
                        "data", null
                ));
            }

            // summary_text에서 수익률 추출
            Double totalReturn = extractReturnRate(latestReport.getSummaryText());

            // DTO를 프론트엔드 응답 형태로 변환
            Map<String, Object> responseData = Map.of(
                    "stats", Map.of(
                            "transactionCount", latestReport.getTransactionCount(),
                            "analysisPeriod", latestReport.getAnalysisPeriod(),
                            "analysisStart", latestReport.getAnalysisStart() != null
                                    ? latestReport.getAnalysisStart().toString()
                                    : "N/A",
                            "analysisEnd", latestReport.getAnalysisEnd() != null
                                    ? latestReport.getAnalysisEnd().toString()
                                    : "N/A",
                            "totalReturn", totalReturn
                    ),
                    "aiAnalysis", Map.of(
                            "strategy", latestReport.getSummaryText(),
                            "risks", latestReport.getRiskText() != null ? latestReport.getRiskText() : "",
                            "advice", latestReport.getSuggestionText() != null ? latestReport.getSuggestionText() : ""
                    )
            );
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