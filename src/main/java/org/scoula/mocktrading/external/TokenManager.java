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

    @Value("${kis.api.app-key}")
    private String appKey;

    @Value("${kis.api.app-secret}")
    private String appSecret;

    @Value("${kis.api.token-url}")
    private String tokenUrl;

    // ✅ 토큰 만료 여유시간 (5분)
    private static final long TOKEN_BUFFER_TIME_MS = 5 * 60 * 1000L;

    public String getAccessToken() throws IOException {
        log.info("🔍 TokenManager.getAccessToken() 시작");

        try {
            String cleanDbUrl = dbUrl.replace("log4jdbc:", "");
            log.info("🔌 DB 연결 시도: {}", cleanDbUrl);

            try (Connection conn = DriverManager.getConnection(cleanDbUrl, dbUser, dbPassword)) {
                log.info("✅ DB 연결 성공");

                // 기존 토큰 확인
                String existingToken = getExistingValidToken(conn);
                if (existingToken != null) {
                    log.info("✅ 유효한 기존 토큰 사용");
                    return existingToken;
                }

                // 새 토큰 발급
                return issueAndStoreNewToken(conn);
            }
        } catch (SQLException e) {
            log.error("❌ DB 접근 실패: {}", e.getMessage());

            // 테이블 존재 여부 확인 및 생성
            if (isTableNotExistError(e)) {
                log.warn("⚠️ token_store 테이블이 없습니다. 자동 생성을 시도합니다.");
                try {
                    createTokenStoreTableIfNotExists();
                    return getAccessToken();
                } catch (SQLException createEx) {
                    log.error("❌ 테이블 생성 실패: {}", createEx.getMessage(), createEx);
                    throw new IOException("테이블 생성 실패: " + createEx.getMessage(), createEx);
                }
            }

            throw new IOException("DB 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ TokenManager 전체 실패: {}", e.getMessage(), e);
            throw new IOException("토큰 매니저 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 기존 유효한 토큰 조회
     */
    private String getExistingValidToken(Connection conn) throws SQLException {
        String sql = "SELECT access_token, expire_time FROM token_store WHERE token_id = 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String token = rs.getString("access_token");
                long expireTime = rs.getLong("expire_time");
                long currentTime = System.currentTimeMillis();

                log.info("🎫 기존 토큰 발견 - 만료시간: {}, 현재시간: {}", expireTime, currentTime);

                if (currentTime < expireTime) {
                    return token;
                } else {
                    log.info("⏰ 토큰 만료됨, 새 토큰 발급 필요");
                }
            } else {
                log.info("🆕 기존 토큰 없음, 새 토큰 발급 필요");
            }
        }

        return null;
    }

    /**
     * 새 토큰 발급 및 저장
     */
    private String issueAndStoreNewToken(Connection conn) throws IOException {
        log.info("🔄 새 토큰 발급 시작");

        // ✅ API 키 검증
        if (appKey == null || appKey.trim().isEmpty() || appKey.contains("your_default")) {
            throw new IOException("KIS API 키가 설정되지 않았습니다. 환경변수 KIS_APP_KEY를 확인하세요.");
        }

        if (appSecret == null || appSecret.trim().isEmpty() || appSecret.contains("your_default")) {
            throw new IOException("KIS API 시크릿이 설정되지 않았습니다. 환경변수 KIS_APP_SECRET을 확인하세요.");
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // 요청 바디 생성
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("grant_type", "client_credentials");
        bodyMap.put("appkey", appKey);
        bodyMap.put("appsecret", appSecret);

        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(bodyMap);

        Request request = new Request.Builder()
                .url(tokenUrl)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                log.error("❌ 토큰 발급 HTTP 실패: {} - {}", response.code(), errorBody);
                throw new IOException("토큰 발급 실패: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            log.info("📨 토큰 API 응답 받음");

            Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);

            String token = (String) responseMap.get("access_token");
            if (token == null || token.trim().isEmpty()) {
                throw new IOException("응답에서 access_token을 찾을 수 없습니다: " + responseBody);
            }

            long expiresInSec = Long.parseLong(responseMap.get("expires_in").toString());
            long expireTime = System.currentTimeMillis() + (expiresInSec - 300) * 1000L; // 5분 여유

            log.info("✅ 새 토큰 발급 성공 - 유효기간: {}초", expiresInSec);

            // DB 저장
            saveTokenToDatabase(conn, token, expireTime);

            return token;

        } catch (SQLException e) {
            log.error("❌ 토큰 DB 저장 실패: {}", e.getMessage(), e);
            throw new IOException("토큰 DB 저장 실패", e);
        }
    }

    /**
     * 토큰을 데이터베이스에 저장
     */
    private void saveTokenToDatabase(Connection conn, String token, long expireTime) throws SQLException {
        String sql = "REPLACE INTO token_store (token_id, access_token, expire_time) VALUES (1, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setLong(2, expireTime);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                log.info("💾 토큰 DB 저장 완료");
            } else {
                log.warn("⚠️ 토큰 DB 저장 실패 - 업데이트된 행이 없음");
            }
        }
    }

    /**
     * token_store 테이블 생성 (MySQL 호환)
     */
    private void createTokenStoreTableIfNotExists() throws SQLException {
        log.info("🔧 token_store 테이블 생성 시도");

        String cleanDbUrl = dbUrl.replace("log4jdbc:", "");
        try (Connection conn = DriverManager.getConnection(cleanDbUrl, dbUser, dbPassword)) {
            // MySQL 호환 테이블 생성 SQL
            String createTableSql = "CREATE TABLE IF NOT EXISTS token_store (" +
                    "token_id INT PRIMARY KEY, " +
                    "access_token TEXT, " +
                    "expire_time BIGINT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
                stmt.executeUpdate();
                log.info("✅ token_store 테이블 생성 완료");
            }
        } catch (SQLException e) {
            log.error("❌ 테이블 생성 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 테이블 존재하지 않는 에러인지 확인
     */
    private boolean isTableNotExistError(SQLException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("doesn't exist") ||
                message.contains("table") && message.contains("token_store") ||
                message.contains("unknown table");
    }
}