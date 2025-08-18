package org.scoula.service.learning;

import org.scoula.domain.learning.dto.ContentDto;
import org.scoula.mapper.learning.LearningContentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningContentServiceImpl extends LearningContentService {

    @Autowired
    private LearningContentMapper contentMapper;

    @Override
    public List<ContentDto> getAllContents() {
        return contentMapper.findAllContents(); // <-- XML에 정의되어 있어야 함
    }

    @Override
    public List<ContentDto> getContentsByRiskType(String riskType) {
        return contentMapper.selectByRiskType(riskType);
    }
}