package org.scoula.service.learning;

import org.scoula.domain.learning.dto.ContentDto;
import org.scoula.mapper.LearningContentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningContentService {

    @Autowired
    private LearningContentMapper contentMapper;

    public List<ContentDto> getContentsByRiskType(String riskType) {
        return contentMapper.selectByRiskType(riskType);
    }
}
