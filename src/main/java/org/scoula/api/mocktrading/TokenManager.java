package org.scoula.api.mocktrading;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.util.mocktrading.ConfigManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Component
public class TokenManager {

    // DB 연결 정보
    private static final String DB_URL = ConfigManager.get("jdbc.url");
    private static final String DB_USER = ConfigManager.get("jdbc.username");
    private static final String DB_PASSWORD = ConfigManager.get("jdbc.password");

    // 한국투자증권 실전투자 API 정보 (2개 키)
    private static final String APP_KEY = ConfigManager.get("app.key");
    private static final String APP_SECRET = ConfigManager.get("app.secret");
    private static final String APP_KEY2 = ConfigManager.get("app.key2");
    private static final String APP_SECRET2 = ConfigManager.get("app.secret2");

    // 키움증권 API 키
        private static final String APP_KEY3 = ConfigManager.get("app.key3");
    private static final String APP_SECRET3 = ConfigManager.get("app.secret3");

    private static final String TOKEN_URL = "https://openapi.koreainvestment.com:9443/oauth2/tokenP";
    private static final String APPROVAL_KEY_URL = "https://openapi.koreainvestment.com:9443/oauth2/Approval";

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

    // ✅ 토큰 종류 구분용 enum
    public enum TokenType {
        MAIN, SUB, KIWOOM
    }

    // ✅ access token만 필요할 때 (기본: MAIN) + SUB 토큰도 함께 발급
    public static String getAccessToken() throws IOException {
        // MAIN 토큰 발급 시 SUB 토큰도 함께 발급받음
        TokenInfo mainToken = getTokenInfo(TokenType.MAIN);

        // SUB 토큰도 백그라운드에서 함께 발급 (실패해도 MAIN 토큰은 정상 반환)
        try {
            getTokenInfo(TokenType.SUB);
            // System.out.println("[TokenManager] MAIN + SUB 토큰 모두 발급 완료");
        } catch (Exception e) {
            System.err.println("[TokenManager] SUB 토큰 발급 실패 (MAIN은 정상): " + e.getMessage());
        }

        return mainToken.getAccessToken();
    }

    // ✅ access token + approval key 모두 필요할 때 (기본: MAIN)
    public static TokenInfo getTokenInfo() throws IOException {
        return getTokenInfo(TokenType.MAIN);
    }

    // ✅ 토큰 종류별로 관리
    public static TokenInfo getTokenInfo(TokenType type) throws IOException {
        String dbKey = "";
        if(type == TokenType.MAIN) {
            dbKey = "1";
        }
        else if (type == TokenType.SUB) {
            dbKey = "2";
        }
        else if (type == TokenType.KIWOOM) {
            dbKey = "3";
        }
        // 한국투자증권 API
        if(dbKey.equals("1") || dbKey.equals("2")) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT access_token, approval_key, expire_time FROM token_store WHERE token_id = ?");
                stmt.setString(1, dbKey);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String token = rs.getString("access_token");
                    String approvalKey = rs.getString("approval_key");
                    long expireTime = rs.getLong("expire_time");

                    if (System.currentTimeMillis() < expireTime) {
                        // System.out.println("[TokenManager] 기존 토큰 사용 (" + type + ")");
                        return new TokenInfo(token, approvalKey, expireTime);
                    }
                }

                // 유효하지 않음 → 새 토큰 발급
                return issueAndStoreNewToken(conn, type, dbKey);

            } catch (SQLException e) {
                throw new IOException("DB 오류", e);
            }
        }
        // 키움 API
        else {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT access_token, expire_time FROM token_store WHERE token_id = ?");
                stmt.setString(1, dbKey);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String token = rs.getString("access_token");
                    long expireTime = rs.getLong("expire_time");
                    if (System.currentTimeMillis() < expireTime) {
                        // 기존 토큰 사용
                        return new TokenInfo(token, null, expireTime);
                    }
                }

                // 유효하지 않음 → 새 토큰 발급
                OkHttpClient client = new OkHttpClient();
                ObjectMapper mapper = new ObjectMapper();

                Map<String, String> bodyMap = new HashMap<>();
                bodyMap.put("grant_type", "client_credentials");
                bodyMap.put("appkey", APP_KEY3);      // 키움 appkey
                bodyMap.put("secretkey", APP_SECRET3); // 키움 secretkey

                String jsonBody = mapper.writeValueAsString(bodyMap);

                Request tokenRequest = new Request.Builder()
                        .url("https://api.kiwoom.com/oauth2/token")
                        .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                        .addHeader("Content-Type", "application/json")
                        .build();

                String token;
                long expireTime;
                try (Response response = client.newCall(tokenRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("키움 access_token 발급 실패: " + response.code());
                    }
                    String responseBody = response.body().string();
                    System.out.println("[TokenManager] 키움 토큰 응답 원문: " + responseBody); // 응답 원문 출력
                    Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);
                    token = (String) responseMap.get("token");
                    Object expiresDtObj = responseMap.get("expires_dt");
                    if (expiresDtObj != null) {
                        // "20250806163448" → Unix timestamp(ms)
                        String expiresDtStr = expiresDtObj.toString();
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
                        sdf.setLenient(false);
                        try {
                            java.util.Date date = sdf.parse(expiresDtStr);
                            expireTime = date.getTime();
                        } catch (java.text.ParseException e) {
                            throw new IOException("expires_dt 파싱 실패: " + expiresDtStr, e);
                        }
                    } else if (responseMap.get("expires_in") != null) {
                        long expiresInSec = Long.parseLong(responseMap.get("expires_in").toString());
                        expireTime = System.currentTimeMillis() + (expiresInSec - 300) * 1000L;
                    } else {
                        throw new IOException("만료 시간 정보가 응답에 없습니다.");
                    }
                }

                // DB 저장 (approval_key는 null)
                try (PreparedStatement update = conn.prepareStatement(
                        "REPLACE INTO token_store (token_id, access_token, approval_key, expire_time) VALUES (?, ?, ?, ?)")) {
                    update.setString(1, dbKey);
                    update.setString(2, token);
                    update.setNull(3, java.sql.Types.VARCHAR); // approval_key는 null
                    update.setLong(4, expireTime);
                    update.executeUpdate();
                } catch (SQLException e) {
                    throw new IOException("키움 토큰 DB 저장 실패", e);
                }

                return new TokenInfo(token, null, expireTime);
            } catch (SQLException e) {
                throw new IOException("키움 DB 오류", e);
            }
        }
    }

    // ✅ 토큰 발급 및 저장 (토큰 종류별)
    private static TokenInfo issueAndStoreNewToken(Connection conn, TokenType type, String dbKey) throws IOException {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // 1. Access Token 요청
        Map<String, String> bodyMap = new HashMap<>();
        String appKey = (type == TokenType.MAIN) ? APP_KEY : APP_KEY2;
        String appSecret = (type == TokenType.MAIN) ? APP_SECRET : APP_SECRET2;
        bodyMap.put("grant_type", "client_credentials");
        bodyMap.put("appkey", appKey);
        bodyMap.put("appsecret", appSecret);

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
            System.out.println("[TokenManager] 토큰 응답 원문 (" + type + "): " + responseBody);

            Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);
            token = (String) responseMap.get("access_token");
            long expiresInSec = Long.parseLong(responseMap.get("expires_in").toString());
            expireTime = System.currentTimeMillis() + (expiresInSec - 300) * 1000L;
        }

        // 2. Approval Key 요청
        String approvalKey = requestApprovalKey(client, appKey, appSecret, type);
        System.out.println("[TokenManager] approval_key 발급 완료 (" + type + "): " + approvalKey);

        // 3. DB 저장 (token_id로 구분)
        try (PreparedStatement update = conn.prepareStatement(
                "REPLACE INTO token_store (token_id, access_token, approval_key, expire_time) VALUES (?, ?, ?, ?)")) {
            update.setString(1, dbKey);
            update.setString(2, token);
            update.setString(3, approvalKey);
            update.setLong(4, expireTime);
            update.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("토큰 DB 저장 실패", e);
        }

        System.out.println("[TokenManager] 새 토큰 + approval_key DB 저장 완료 (" + type + ")");
        return new TokenInfo(token, approvalKey, expireTime);
    }

    // ✅ Approval Key 요청 (토큰 종류별)
    private static String requestApprovalKey(OkHttpClient client, String appKey, String appSecret, TokenType type) throws IOException {
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("grant_type", "client_credentials");
        bodyMap.put("appkey", appKey);
        bodyMap.put("secretkey", appSecret);

        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(bodyMap);

        Request request = new Request.Builder()
                .url(APPROVAL_KEY_URL)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println("[TokenManager] approval 응답 (" + type + "): " + responseBody);

            if (!response.isSuccessful()) {
                throw new IOException("approval_key 요청 실패: " + response.code());
            }

            Map<String, Object> map = mapper.readValue(responseBody, Map.class);
            return (String) map.get("approval_key");
        }
    }



}



