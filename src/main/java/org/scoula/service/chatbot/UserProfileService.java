package org.scoula.service.chatbot;

import lombok.extern.log4j.Log4j2;
import org.scoula.mapper.chatbot.InvestmentTypeMapper;
import org.scoula.service.Auth.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

@Log4j2
@Service
@RequestMapping
public class UserProfileService {

    private final UserService userService;
    private final InvestmentTypeMapper investmentTypeMapper;

    /**
     *   UserProfileService
     * - 사용자 프로필(특히 투자 성향) 관련 데이터를 조회/활용하는 서비스
     * - 현재는 riskType(투자 성향 코드) 조회 기능만 제공
     * - 추후 investmentTypeMapper 등을 활용해 성향 기반 상세 분석/추천으로 확장 가능
     */
    public UserProfileService(UserService userService, InvestmentTypeMapper investmentTypeMapper) {
        this.userService = userService;
        this.investmentTypeMapper = investmentTypeMapper;
    }


    /**
     *  사용자 ID 기반 투자 성향 조회
     *
     * @param userId 사용자 PK
     * @return String riskType (예: "RISK_AVERSE", "AGGRESSIVE" 등)
     *
     * - 내부적으로 Auth.UserService 를 호출하여 DB에서 riskType 값을 가져옴
     * - chatbot 프롬프트에서 사용자 맞춤형 답변을 생성할 때 활용됨
     */
    public String getRiskTypeByUserId(Integer userId) {
        return userService.getRiskTypeByUserId(userId);
    }
}
