package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.learning.dto.ContentDto;

import java.util.List;

@Mapper
public interface LearningContentMapper {
    List<ContentDto> selectByRiskType(@Param("riskType") String riskType);
    List<ContentDto> findAllContents();
    ContentDto findById(@Param("id") Long id);
}
