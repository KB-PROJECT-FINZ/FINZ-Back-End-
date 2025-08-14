package org.scoula.service.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.feedback.dto.AIAnalysisReportDto;
import org.scoula.mapper.AIAnalysisReportMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 분석 리포트 서비스
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class AIAnalysisReportService {

    private final AIAnalysisReportMapper aiAnalysisReportMapper;

    /**
     * 사용자 ID로 최신 AI 분석 리포트 조회
     * @param userId 사용자 ID
     * @return 최신 AI 분석 리포트 (없으면 null)
     */
    public AIAnalysisReportDto getLatestByUserId(Integer userId) {
        try {
            AIAnalysisReportDto report = aiAnalysisReportMapper.selectLatestByUserId(userId);

            if (report != null) {
                log.info("AI 분석 리포트 조회 성공 - 리포트 ID: {}, 생성일: {}",
                        report.getId(), report.getGeneratedAt());
            } else {
                log.info("사용자 {}의 AI 분석 리포트가 존재하지 않음", userId);
            }

            return report;

        } catch (Exception e) {
            log.error("AI 분석 리포트 조회 실패 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("AI 분석 리포트 조회 중 오류가 발생했습니다.", e);
        }
    }
}