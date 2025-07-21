package org.scoula.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Log4j2
public class HomeController {

    @GetMapping("/")
    public String home() {
        log.info("====================> HomeController /");
        return "index";         // View의 이름
    }
    
    @GetMapping("/api-docs")
    public String apiDocs() {
        log.info("====================> Redirecting to Swagger UI");
        return "redirect:/swagger-ui.html";
    }
}
