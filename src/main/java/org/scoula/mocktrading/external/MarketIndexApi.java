package org.scoula.mocktrading.external;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class MarketIndexApi {

    @Autowired
    private TokenManager tokenManager;

    // ✅ 환경변수로 관리되는 설정값들
    @Value("${kis.api.app-key}")
    private String appKey;

    @Value("${kis.api.app-secret}")
    private String appSecret;

    @Value("${kis.api.base-url}")
    private String baseUrl;

    // ✅ API 호출 간격 (밀리초)
    private static final long API_CALL_INTERVAL_MS = 1000L;

    /**
     * 시장지수 조회 - 개선된 버전
     * @param indexCode "0001"(코스피), "1001"(코스닥)
     * @return 시장지수 데이터
     */
    public Map<String, Object> getMarketIndex(String indexCode) throws IOException {
        log.info("🔍 시장지수 조회 시작 - 코드: {}", indexCode);

        // ✅ API 키 검증
        validateApiCredentials();

        String token = tokenManager.getAccessToken();

        HttpUrl url = HttpUrl.parse(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-index-price")
                .newBuilder()
                .addQueryParameter("FID_COND_MRKT_DIV_CODE", "U")
                .addQueryParameter("FID_INPUT_ISCD", indexCode)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("appkey", appKey)
                .addHeader("appsecret", appSecret)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("tr_id", "FHPUP02100000")
                .addHeader("custtype", "P")
                .build();

        // ✅ 타임아웃 설정이 있는 클라이언트
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                log.error("❌ 시장지수 조회 HTTP 실패: {} - {}", response.code(), errorBody);
                throw new IOException("시장지수 조회 실패: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            log.debug("📨 시장지수 API 응답 받음: {}", responseBody.substring(0, Math.min(200, responseBody.length())));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(responseBody);

            String rtCd = json.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String errorMsg = json.path("msg1").asText();
                log.error("❌ 시장지수 조회 API 오류: {}", errorMsg);
                throw new IOException("API 오류: " + errorMsg);
            }

            JsonNode output = json.path("output");
            Map<String, Object> result = parseIndexData(output, indexCode);

            log.info("✅ 시장지수 조회 성공 - {}: {}", result.get("name"), result.get("value"));
            return result;

        } catch (IOException e) {
            log.error("❌ 시장지수 조회 실패 - 코드: {}, 오류: {}", indexCode, e.getMessage());
            throw e;
        }
    }

    /**
     * 코스피/코스닥 둘 다 조회 - 개선된 버전
     */
    public Map<String, Object> getAllMarketIndices() throws IOException {
        log.info("🔍 전체 시장지수 조회 시작");

        Map<String, Object> result = new HashMap<>();

        try {
            // 코스피 조회
            log.info("📊 코스피 지수 조회");
            result.put("kospi", getMarketIndex("0001"));

            // API 호출 간격 준수
            waitForApiInterval();

            // 코스닥 조회
            log.info("📊 코스닥 지수 조회");
            result.put("kosdaq", getMarketIndex("1001"));

            log.info("✅ 전체 시장지수 조회 완료");
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ API 호출 중 인터럽트 발생");
            throw new IOException("API 호출 중 인터럽트", e);
        } catch (IOException e) {
            log.error("❌ 전체 시장지수 조회 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * JSON 응답 데이터 파싱
     */
    private Map<String, Object> parseIndexData(JsonNode output, String indexCode) {
        Map<String, Object> result = new HashMap<>();

        JsonNode indexData = output.isArray() ? output.get(0) : output;

        // ✅ 안전한 필드 값 추출
        String currentPrice = getFieldValue(indexData, "bstp_nmix_prpr", "stck_prpr", "current_price");
        String change = getFieldValue(indexData, "bstp_nmix_prdy_vrss", "prdy_vrss", "change");
        String changeRate = getFieldValue(indexData, "bstp_nmix_prdy_ctrt", "prdy_ctrt", "change_rate");
        String changeSign = getFieldValue(indexData, "prdy_vrss_sign", "sign", "change_sign");

        // ✅ 데이터 검증 및 로깅
        log.debug("📈 파싱된 데이터 - 현재가: {}, 변동: {}, 변동률: {}, 부호: {}",
                currentPrice, change, changeRate, changeSign);

        result.put("name", getIndexName(indexCode));
        result.put("value", parseDouble(currentPrice));
        result.put("change", parseDouble(change));
        result.put("changePercent", parseDouble(changeRate));
        result.put("isPositive", isPositiveChange(changeSign));

        return result;
    }

    /**
     * 여러 필드명을 시도해서 값을 가져오는 헬퍼 메서드
     */
    private String getFieldValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.asText().isEmpty()) {
                return field.asText();
            }
        }
        log.warn("⚠️ 필드를 찾을 수 없음: {}", String.join(", ", fieldNames));
        return "0";
    }

    /**
     * 문자열을 double로 안전하게 파싱
     */
    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("⚠️ 숫자 파싱 실패: '{}' -> 0.0으로 대체", value);
            return 0.0;
        }
    }

    /**
     * 등락 부호로 양수/음수 판별
     */
    private boolean isPositiveChange(String changeSign) {
        if (changeSign == null || changeSign.trim().isEmpty()) {
            return false;
        }

        // KIS API 등락 부호: 1,2=상승, 3=보합, 4,5=하락
        String sign = changeSign.trim();
        boolean isPositive = "1".equals(sign) || "2".equals(sign);

        log.debug("📊 등락 부호 해석: '{}' -> {}", sign, isPositive ? "상승" : "하락");
        return isPositive;
    }

    /**
     * 지수 코드로 지수명 반환
     */
    private String getIndexName(String indexCode) {
        switch (indexCode) {
            case "0001":
                return "KOSPI";
            case "1001":
                return "KOSDAQ";
            default:
                log.warn("⚠️ 알 수 없는 지수 코드: {}", indexCode);
                return "UNKNOWN";
        }
    }

    /**
     * API 호출 간격 대기
     */
    private void waitForApiInterval() throws InterruptedException {
        log.debug("⏳ API 호출 간격 대기: {}ms", API_CALL_INTERVAL_MS);
        Thread.sleep(API_CALL_INTERVAL_MS);
    }

    /**
     * API 인증정보 검증
     */
    private void validateApiCredentials() throws IOException {
        if (appKey == null || appKey.trim().isEmpty() || appKey.contains("your_default")) {
            throw new IOException("KIS API 키가 설정되지 않았습니다. 환경변수를 확인하세요.");
        }

        if (appSecret == null || appSecret.trim().isEmpty() || appSecret.contains("your_default")) {
            throw new IOException("KIS API 시크릿이 설정되지 않았습니다. 환경변수를 확인하세요.");
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IOException("KIS API URL이 설정되지 않았습니다.");
        }
    }
}