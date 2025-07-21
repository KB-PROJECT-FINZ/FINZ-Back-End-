package org.scoula.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.scoula.controller"))
                .paths(PathSelectors.regex("/api/.*"))
                .build()
                .apiInfo(apiInfo())
                .useDefaultResponseMessages(false) // 기본 응답 메시지 사용하지 않음
                .forCodeGeneration(true); // 코드 생성을 위한 최적화
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("FINZ Mock Trading API")
                .description("FINZ 모의투자 시스템 REST API 문서\n\n" +
                           "주요 기능:\n" +
                           "- 실시간 주가 조회\n" +
                           "- 모의 매수/매도\n" +
                           "- AI 챗봇 상담")
                .version("1.0.0")
                .contact(new Contact("FINZ Team", "https://github.com/KB-PROJECT-FINZ", "finz@kb.com"))
                .license("Apache License Version 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .build();
    }
}