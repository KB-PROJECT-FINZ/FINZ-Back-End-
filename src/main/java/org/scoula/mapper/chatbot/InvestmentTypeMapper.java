package org.scoula.mapper.chatbot;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.chatbot.dto.InvestmentTypeDto;

@Mapper
public interface InvestmentTypeMapper {
    // 리스크타입 다 들고오는 mapper
    InvestmentTypeDto findByRiskType(@Param("riskType") String riskType);

}
