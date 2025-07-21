package org.scoula.api.mocktrading;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class TokenManager {

    // DB 연결 정보
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mock_trading_db";
    private static final String DB_USER = "mockuser";
    private static final String DB_PASSWORD = "mockpassword";

    // 한국투자증권 모의투자 API 정보
    private static final String APP_KEY = "PSH0easBeXEsg1O8xDFID806ogGp5m9OnDE4";
    private static final String APP_SECRET = "qebHXATUH/Y8lxudeqIguM2F3F8TP0zedOogN9bYnTdhjdKGnEj2tfY1imFgWQw3dle/oB3m6Mde6x+LTrC2RSQtYOXdD6kiByz4hWZmZFhCDOuU4LEuA0dYNaCG3pSfUT0q/+9d3KHVobbj9wNylWv4QLGT6CTc04aU3UPootqzYzeIh+M=";
    private static final String TOKEN_URL = "https://openapivts.koreainvestment.com:29443/oauth2/tokenP";
    private static final String APPROVAL_KEY_URL = "https://openapivts.koreainvestment.com:29443/oauth2/Approval";

    // ✅ 토큰 정보 클래스
    public static class TokenInfo {
        private final String accessToken;
        private final String approvalKey;
        private final long expireTime;

        public TokenInfo(String accessToken, String approvalKey, long expireTime) {
            this.accessToken = accessToken;
            this.approvalKey = approvalKey;
            this.expireTime = expireTime;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getApprovalKey() {
            return approvalKey;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }

    // ✅ access token만 필요할 때
    public static String getAccessToken() throws IOException {
        return getTokenInfo().getAccessToken();
    }

    // ✅ access token + approval key 모두 필요할 때
    public static TokenInfo getTokenInfo() throws IOException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT access_token, approval_key, expire_time FROM token_store WHERE token_id = 1");
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String token = rs.getString("access_token");
                String approvalKey = rs.getString("approval_key");
                long expireTime = rs.getLong("expire_time");

                if (System.currentTimeMillis() < expireTime) {
                    System.out.println("[TokenManager] 기존 토큰 사용");
                    return new TokenInfo(token, approvalKey, expireTime);
                }
            }

            // 유효하지 않음 → 새 토큰 발급
            return issueAndStoreNewToken(conn);

        } catch (SQLException e) {
            throw new IOException("DB 오류", e);
        }
    }

    // ✅ 토큰 발급 및 저장
    private static TokenInfo issueAndStoreNewToken(Connection conn) throws IOException {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // 1. Access Token 요청
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("grant_type", "client_credentials");
        bodyMap.put("appkey", APP_KEY);
        bodyMap.put("appsecret", APP_SECRET);

        String jsonBody = mapper.writeValueAsString(bodyMap);

        Request tokenRequest = new Request.Builder()
                .url(TOKEN_URL)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

        String token;
        long expireTime;
        try (Response response = client.newCall(tokenRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("access_token 발급 실패: " + response.code());
            }

            String responseBody = response.body().string();
            System.out.println("[TokenManager] 토큰 응답 원문: " + responseBody);

            Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);
            token = (String) responseMap.get("access_token");
            long expiresInSec = Long.parseLong(responseMap.get("expires_in").toString());
            expireTime = System.currentTimeMillis() + (expiresInSec - 300) * 1000L;
        }

        // 2. Approval Key 요청
        String approvalKey = requestApprovalKey(client);
        System.out.println("[TokenManager] approval_key 발급 완료: " + approvalKey);

        // 3. DB 저장
        try (PreparedStatement update = conn.prepareStatement(
                "REPLACE INTO token_store (token_id, access_token, approval_key, expire_time) VALUES (1, ?, ?, ?)")) {
            update.setString(1, token);
            update.setString(2, approvalKey);
            update.setLong(3, expireTime);
            update.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("토큰 DB 저장 실패", e);
        }

        System.out.println("[TokenManager] 새 토큰 + approval_key DB 저장 완료");
        return new TokenInfo(token, approvalKey, expireTime);
    }

    // ✅ Approval Key 요청
    private static String requestApprovalKey(OkHttpClient client) throws IOException {
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("grant_type", "client_credentials");
        bodyMap.put("appkey", APP_KEY);
        bodyMap.put("secretkey", APP_SECRET);  // ✅ 정확히 'secretkey'

        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(bodyMap);

        Request request = new Request.Builder()
                .url("https://openapivts.koreainvestment.com:29443/oauth2/Approval")  // ✅ 올바른 URL
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json; charset=utf-8")  // ✅ 정확한 헤더
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println("[TokenManager] approval 응답: " + responseBody);

            if (!response.isSuccessful()) {
                throw new IOException("approval_key 요청 실패: " + response.code());
            }

            Map<String, Object> map = mapper.readValue(responseBody, Map.class);
            return (String) map.get("approval_key");  // ✅ approval_key는 최상단에 위치
        }
    }

}




