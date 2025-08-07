package org.scoula.config;

import org.scoula.util.mocktrading.ConfigManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Component
public class StartupSqlRunner implements ApplicationListener<ContextRefreshedEvent> {

    private boolean alreadyStarted = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!alreadyStarted && event.getApplicationContext().getParent() == null) {
            alreadyStarted = true;
            System.out.println("[서버 구동 이후 SQL문 실행 시작]");

            try (Connection conn = DriverManager.getConnection(
                    ConfigManager.get("jdbc.url"),
                    ConfigManager.get("jdbc.username"),
                    ConfigManager.get("jdbc.password"));
                 Statement stmt = conn.createStatement()) {

                // 여기에 SQL문 작성
                stmt.executeUpdate("DELETE FROM pending_orders\n" +
                        "WHERE DATE(created_at) <> CURDATE();");

                System.out.println("[서버 구동 이후 SQL문 실행 완료]");
            } catch (Exception e) {
                System.err.println("[서버 구동 이후 SQL문 실행 오류]: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}