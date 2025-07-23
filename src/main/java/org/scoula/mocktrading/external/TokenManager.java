package org.scoula.mocktrading.external;

import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Component
@Log4j2
public class TokenManager {

    @Value("${jdbc.url}")
    private String dbUrl;

    @Value("${jdbc.username}")
    private String dbUser;

    @Value("${jdbc.password}")
    private String dbPassword;

    private static final String APP_KEY = "PS4ZWuN1nszCbgArV5ZcccGwkvGiIwWo9533";
    private static final String APP_SECRET = "3Rs9bawB5FFzLFFa11naf0Jx4JyYqEESiPKR2sLTtJm3IogkA833HpBzqTnbOYGd+9AcLGVA2Z22V1oXXY4z4zug1tPxs7UU44fgSg3KzuYiwZ33qwAPqd4Cm3RohhjgNS2o9mNLAyev0mOL9QTwKTgfItpK7VgGYqE42VDo8tHwBb7pRAM=";
    private static final String TOKEN_URL = "https://openapivts.koreainvestment.com:29443/oauth2/tokenP";

    public String getAccessToken() throws IOException {
        log.info("🔍 TokenManager.getAccessToken() 시작");
        log.info("🗄️ DB 연결 정보 - URL: {}, User: {}", dbUrl, dbUser);

        try {
            String cleanDbUrl = dbUrl.replace("log4jdbc:", "");
            log.info("🔌 실제 DB URL: {}", cleanDbUrl);

            try (Connection conn = DriverManager.getConnection(cleanDbUrl, dbUser, dbPassword)) {
                log.info("✅ DB 연결 성공");

                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT access_token, expire_time FROM token_store WHERE id = 1");
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String token = rs.getString("access_token");
                    long expireTime = rs.getLong("expire_time");

                    log.info("🎫 기존 토큰 발견 - 만료시간: {}", expireTime);
                    log.info("⏰ 현재시간: {}", System.currentTimeMillis());

                    if (System.currentTimeMillis() < expireTime) {
                        log.info("✅ 기존 토큰 사용");
                        return token;
                    } else {
                        log.info("⏰ 토큰 만료됨, 새 토큰 발급 필요");
                    }
                } else {
                    log.info("🆕 기존 토큰 없음, 새 토큰 발급 필요");
                }

                return issueAndStoreNewToken(conn);
            }
        } catch (SQLException e) {
            log.error("❌ DB 접근 실패: {}", e.getMessage(), e);

            // 테이블이 없는 경우 자동 생성 시도
            if (e.getMessage().contains("doesn't exist") || e.getMessage().contains("Table") || e.getMessage().contains("token_store")) {
                log.warn("⚠️ token_store 테이블이 없는 것 같습니다. 자동 생성을 시도합니다.");
                try {
                    createTokenStoreTable();
                    return getAccessToken(); // 재귀 호출
                } catch (Exception createEx) {
                    log.error("❌ 테이블 생성 실패: {}", createEx.getMessage(), createEx);
                }
            }

            throw new IOException("DB 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ TokenManager 전체 실패: {}", e.getMessage(), e);
            throw new IOException("토큰 매니저 오류: " + e.getMessage(), e);
        }
    }

    /**
     * token_store 테이블 자동 생성
     */
    private void createTokenStoreTable() throws SQLException {
        log.info("🔧 token_store 테이블 생성 시도");

        String cleanDbUrl = dbUrl.replace("log4jdbc:", "");
        try (Connection conn = DriverManager.getConnection(cleanDbUrl, dbUser, dbPassword)) {
            String createTableSql = "CREATE TABLE IF NOT EXISTS token_store (" +
                    "id INT PRIMARY KEY, " +
                    "access_token TEXT, " +
                    "expire_time BIGINT" +
                    ")";

            try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
                stmt.executeUpdate();
                log.info("✅ token_store 테이블 생성 완료");
            }
        }
    }

    private String issueAndStoreNewToken(Connection conn) throws IOException {
        log.info("🔄 새 토큰 발급 시작");

        OkHttpClient client = new OkHttpClient();

        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("grant_type", "client_credentials");
        bodyMap.put("appkey", APP_KEY);
        bodyMap.put("appsecret", APP_SECRET);

        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(bodyMap);

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("❌ 토큰 발급 HTTP 실패: {}", response.code());
                throw new IOException("토큰 발급 실패: " + response.code());
            }

            String responseBody = response.body().string();
            log.info("📨 토큰 API 응답 받음");

            Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);

            String token = (String) responseMap.get("access_token");
            long expiresInSec = Long.parseLong(responseMap.get("expires_in").toString());
            long expireTime = System.currentTimeMillis() + (expiresInSec - 300) * 1000L; // 5분 여유

            log.info("✅ 새 토큰 발급 성공 - 유효기간: {}초", expiresInSec);

            // DB 저장
            try (PreparedStatement update = conn.prepareStatement(
                    "REPLACE INTO token_store (id, access_token, expire_time) VALUES (1, ?, ?)")) {
                update.setString(1, token);
                update.setLong(2, expireTime);
                update.executeUpdate();

                log.info("💾 토큰 DB 저장 완료");
            }

            return token;

        } catch (SQLException e) {
            log.error("❌ 토큰 DB 저장 실패: {}", e.getMessage(), e);
            throw new IOException("토큰 DB 저장 실패", e);
        }
    }
}