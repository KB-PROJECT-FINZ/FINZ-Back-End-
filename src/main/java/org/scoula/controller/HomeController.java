package org.scoula.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@Log4j2
public class HomeController {

    /**
     * 홈페이지 - Vue.js 앱으로 포워딩
     */
    @GetMapping("/")
    public String home() {
        log.info("====================> HomeController / -> Vue.js");
        return "forward:/resources/index.html";
    }
    @GetMapping("/api-docs")
    public String apiDocs() {
        log.info("====================> Redirecting to Swagger UI");
        return "redirect:/swagger-ui.html";
    }
}





