package org.scoula.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.*;
import java.util.Arrays;

public class WebConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    // OS에 맞게 경로 변경 필요함 - 멀티 파트 관련
    final String LOCATION = "C:/upload";
    final long MAX_FILE_SIZE = 1024 * 1024 * 10L;
    final long MAX_REQUEST_SIZE = 1024 * 1024 * 20L;
    final int FILE_SIZE_THRESHOLD = 1024 * 1024 * 5;

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[] {RootConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] {ServletConfig.class};
    }

    // 중요: 모든 요청을 DispatcherServlet이 처리하도록 설정
    @Override
    protected String[] getServletMappings() {
        return new String[] {"/"};  // 모든 요청 매핑
    }

    // POST body 문자 인코딩 필터 설정 - UTF-8 설정
    @Override
    protected Filter[] getServletFilters() {
        // 문자 인코딩 필터
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();

        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return new Filter[] {characterEncodingFilter};
    }

    // 멀티 파트 관련 + 404에러 처리 관련 설정
    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // 404 에러 처리를 위한 설정
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");

        MultipartConfigElement multipartConfig = new MultipartConfigElement(
                LOCATION,               // 업로드 처리 디렉터리 경로
                MAX_FILE_SIZE,          // 업로드 가능한 파일 하나의 최대 크기
                MAX_REQUEST_SIZE,       // 업로드 가능한 전체 최대 크기
                FILE_SIZE_THRESHOLD     // 메모리 파일의 최대 크기
        );
        registration.setMultipartConfig(multipartConfig);

        // 디버깅을 위한 로그
        System.out.println("=== WebConfig 초기화 ===");
        System.out.println("Servlet Mappings: " + String.join(", ", getServletMappings()));
        System.out.println("Root Config Classes: " + java.util.Arrays.toString(getRootConfigClasses()));
        System.out.println("Servlet Config Classes: " + java.util.Arrays.toString(getServletConfigClasses()));
        System.out.println("========================");
    }
    @Configuration
    public class CorsConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:5173") // Vue 개발 서버 주소
                    .allowedMethods("*")
                    .allowCredentials(true); // 세션 쿠키 전송 허용
        }
    }
}
