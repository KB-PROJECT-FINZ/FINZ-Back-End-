package org.scoula.service.learning;

import org.scoula.domain.learning.dto.ContentDto;
import org.scoula.mapper.LearningContentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public  class LearningContentService {

    @Autowired
    private LearningContentMapper contentMapper;

    public List<ContentDto> getAllContents() {
        return contentMapper.findAllContents();
    }

    public List<ContentDto> getContentsByRiskType(String riskType) {
        return contentMapper.selectByRiskType(riskType);
    }

    public ContentDto getContentById(Long id) {
        ContentDto dto = contentMapper.findById(id);
        System.out.println("콘텐츠 조회 결과: " + dto); // 로그 추가
        return dto;
    }

}
