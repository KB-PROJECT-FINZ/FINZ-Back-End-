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

    public String getRiskTypeByUserId(Integer userId) {
        return userService.getRiskTypeByUserId(userId);
    }
}
