package org.scoula.config;

import org.scoula.util.mocktrading.ConfigManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.sql.*;

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
                    ConfigManager.get("jdbc.password"))) {

                // 1. 오늘이 아닌 모든 pending_orders 조회
                String selectSql = "SELECT order_id, account_id, stock_code, order_type, quantity, target_price FROM pending_orders WHERE DATE(created_at) <> CURDATE()";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(selectSql)) {

                    while (rs.next()) {
                        int accountId = rs.getInt("account_id");
                        String stockCode = rs.getString("stock_code");
                        String orderType = rs.getString("order_type");
                        int quantity = rs.getInt("quantity");
                        int targetPrice = rs.getInt("target_price");

                        if ("BUY".equalsIgnoreCase(orderType)) {
                            // 현금 환불
                            String updateCash = "UPDATE user_accounts SET current_balance = current_balance + ? WHERE account_id = ?";
                            try (PreparedStatement pstmt = conn.prepareStatement(updateCash)) {
                                pstmt.setLong(1, (long) quantity * targetPrice);
                                pstmt.setInt(2, accountId);
                                pstmt.executeUpdate();
                            }
                        } else if ("SELL".equalsIgnoreCase(orderType)) {
                            // 주식 수량 환불
                            String updateStock = "UPDATE holdings SET quantity = quantity + ? WHERE account_id = ? AND stock_code = ?";
                            try (PreparedStatement pstmt = conn.prepareStatement(updateStock)) {
                                pstmt.setInt(1, quantity);
                                pstmt.setInt(2, accountId);
                                pstmt.setString(3, stockCode);
                                pstmt.executeUpdate();
                            }
                        }
                    }
                }

                // 2. 이제 안전하게 삭제
                String deleteSql = "DELETE FROM pending_orders WHERE DATE(created_at) <> CURDATE()";
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(deleteSql);
                }
            } catch (SQLException e) {
                System.err.println("[StartupSqlRunner] SQL 실행 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("[StartupSqlRunner] 예기치 않은 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}