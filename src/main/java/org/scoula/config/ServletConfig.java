package org.scoula.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.websocket.server.ServerContainer;

@EnableWebMvc
@ComponentScan(basePackages = {"org.scoula"})        // Spring MVC용 컴포넌트 등록을 위한 스캔 패키지
public class ServletConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")    // url이 /resources/로 시작하는 모든 경로
            .addResourceLocations("/resources/");       // webapp/resources/ 경로로 매핑
        
        // Swagger UI 리소스 핸들러 추가
        registry.addResourceHandler("swagger-ui.html")
            .addResourceLocations("classpath:/META-INF/resources/");
        
        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    // CORS 설정 추가
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // jsp view resolver 설정
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

//    @Override
//    public void onStartup(ServletContext servletContext) throws ServletException {
//        // 기존 설정
//        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
//        context.register(RootConfig.class);
//        servletContext.addListener(new ContextLoaderListener(context));
//
//        ServletRegistration.Dynamic dispatcher = servletContext.addServlet("dispatcher",
//                new DispatcherServlet(context));
//        dispatcher.setLoadOnStartup(1);
//        dispatcher.addMapping("/");
//
//        // ✅ WebSocket 엔드포인트 수동 등록
//        try {
//            ServerContainer serverContainer =
//                    (ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");
//
//            if (serverContainer != null) {
//                serverContainer.addEndpoint(Class.forName("org.scoula.controller.mocktrading.StockRelaySocket"));
//                System.out.println("✅ WebSocket 엔드포인트 등록 완료 (/ws/stock)");
//            } else {
//                System.err.println("❌ WebSocket ServerContainer가 null입니다.");
//            }
//        } catch (Exception e) {
//            System.err.println("❌ WebSocket 등록 중 예외 발생: " + e.getMessage());
//        }
//    }

}
