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

    /**
     * Vue Router 경로들을 index.html로 포워딩
     * Vue.js SPA 라우팅 지원
     */
    @RequestMapping(value = {
            "/mock-trading",
            "/mock-trading/**"
    })
    public String mockTrading(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.info("Vue Router: {} -> index.html", path);
        return "forward:/resources/index.html";

    }

    @GetMapping("/api-docs")
    public String apiDocs() {
        log.info("====================> Redirecting to Swagger UI");
        return "redirect:/swagger-ui.html";
    }
}
