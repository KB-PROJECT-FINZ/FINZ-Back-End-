package org.scoula.config;

import org.scoula.config.auth.LoginUserArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.websocket.server.ServerContainer;
import java.util.List;

@EnableWebMvc
//@ComponentScan(basePackages = {
//        "org.scoula"
//})
//@ComponentScan(basePackages = {"org.scoula"})
// Spring MVC용 컴포넌트 등록을 위한 스캔 패키지
public class ServletConfig implements WebMvcConfigurer {

    // 뷰 컨트롤러 제거 - 리소스 핸들러로 처리

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 모든 경로를 정적 리소스에서 우선 처리
    registry.addResourceHandler("/**")
            .addResourceLocations("/resources/")
            .setCachePeriod(0)
            .resourceChain(true)
            .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver() {
                @Override
                protected org.springframework.core.io.Resource getResource(String resourcePath, org.springframework.core.io.Resource location) throws java.io.IOException {
                    org.springframework.core.io.Resource requestedResource = super.getResource(resourcePath, location);
                    // 실제 파일이 있으면 그대로 반환, 없으면 index.html 반환
                    return requestedResource != null ? requestedResource : location.createRelative("index.html");
                }
            });
        
        // 기본 리소스 핸들러 (develop 브랜치에서)
        registry.addResourceHandler("/resources/**")    // url이 /resources/로 시작하는 모든 경로
                .addResourceLocations("/resources/");       // webapp/resources/ 경로로 매핑

        // Vue.js 에셋 파일을 위한 핸들러 (HEAD 브랜치에서)
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("/resources/assets/");

        // Swagger UI 리소스를 위한 핸들러 설정 (두 브랜치 모두 포함, 더 완전한 설정 사용)
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        // Swagger WebJar 리소스 설정
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        // 추가 Swagger 리소스 설정 (HEAD 브랜치에서)
        registry.addResourceHandler("/swagger-resources/**")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/v2/api-docs")
                .addResourceLocations("classpath:/META-INF/resources/");
    }

    // CORS 설정 추가 (develop 브랜치에서)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 모든 경로에 대해 CORS 허용
                .allowedOriginPatterns("*")  // allowCredentials=true일 때는 allowedOriginPatterns 사용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(false)  // credentials를 false로 설정
                .maxAge(3600);
    }

    // jsp view resolver 설정 (develop 브랜치에서)
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        InternalResourceViewResolver bean = new InternalResourceViewResolver();

        bean.setViewClass(JstlView.class);
        bean.setPrefix("/WEB-INF/views/");
        bean.setSuffix(".jsp");

        registry.viewResolver(bean);
    }

    // Servlet 3.0 파일 업로드 사용 시 - MultipartResolver 빈 등록
    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        return resolver;
    }

    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginUserArgumentResolver());
    }

}
