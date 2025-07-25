package org.scoula.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Configuration
@PropertySource({"classpath:/application.properties"})
@EnableScheduling
@ComponentScan(basePackages = {
        // develop 브랜치의 패키지들 (서비스 레이어, API 클라이언트, 설정 클래스)
        "org.scoula.service",           // 서비스 레이어
        "org.scoula.api",              // API 클라이언트들
        "org.scoula.config",           // 설정 클래스들
        // HEAD 브랜치의 패키지들 (mocktrading 관련 기능)
        "org.scoula.mocktrading.service",
        "org.scoula.mocktrading.external"
})
@MapperScan(basePackages = {"org.scoula.mapper"})
@Import(SwaggerConfig.class)  // SwaggerConfig 추가
//@MapperScan(basePackages = {})
public class RootConfig {
    @Value("${jdbc.driver}")
    String driver;

    @Value("${jdbc.url}")
    String url;

    @Value("${jdbc.username}")
    String username;

    @Value("${jdbc.password}")
    String password;

    @Autowired
    ApplicationContext applicationContext;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // 👉 커넥션 풀 제한 설정 추가!
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);



        HikariDataSource dataSource = new HikariDataSource(config);

        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        sqlSessionFactory.setDataSource(dataSource());
        return (SqlSessionFactory) sqlSessionFactory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());
        return manager;
    }
}
