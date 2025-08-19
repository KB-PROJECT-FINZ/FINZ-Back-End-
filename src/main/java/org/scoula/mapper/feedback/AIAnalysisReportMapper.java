package org.scoula.mapper.feedback;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.feedback.dto.AIAnalysisReportDto;

/**
 * AI 분석 리포트 Mapper
 * ai_analysis_reports 테이블과 매핑
 */
@Mapper
public interface AIAnalysisReportMapper {
    AIAnalysisReportDto selectLatestByUserId(@Param("userId") Integer userId);
}