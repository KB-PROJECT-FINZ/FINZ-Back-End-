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

    public UserProfileService(UserService userService, InvestmentTypeMapper investmentTypeMapper) {
        this.userService = userService;
        this.investmentTypeMapper = investmentTypeMapper;
    }

    public String buildProfileSummary(String username) {
        String riskType = userService.getRiskTypeNameByUsername(username);
        if (riskType == null) return "성향 정보 없음";

        var dto = investmentTypeMapper.findByRiskType(riskType);
        return dto != null ? dto.getDescription() : "성향 정보 없음";
    }
    public String buildProfileSummaryByUserId(Integer userId) {
        String riskType = userService.getRiskTypeByUserId(userId);
        log.info("🔍 [UserProfile] userId={} -> riskType={}", userId, riskType);

        var dto = investmentTypeMapper.findByRiskType(riskType);
        log.info("🔍 [UserProfile] riskType={} -> dto={}", riskType, dto);

        return dto != null ? dto.getDescription() : "성향 정보 없음";
    }
    public String getRiskTypeByUserId(Integer userId) {
        return userService.getRiskTypeByUserId(userId);
    }
}
