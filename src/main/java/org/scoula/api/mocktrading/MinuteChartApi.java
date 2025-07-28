package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.scoula.api.mocktrading.TokenManager;
import org.scoula.util.mocktrading.ConfigManager;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class MinuteChartApi {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConfigManager configManager = new ConfigManager();

    /**
     * 분봉 차트 데이터 조회 - 원본 API 응답 반환
     *
     * @param stockCode 종목코드
     * @return 한국투자증권 API 원본 응답 데이터
     */
    public JsonNode getRawMinuteChartData(String stockCode) {
        log.info("Fetching raw minute chart data for stock: {}", stockCode);

        try {
            // 한국투자증권 API 호출하여 원본 응답 반환
            JsonNode response = fetchRawMinuteData(stockCode);

            if (response == null) {
                log.warn("No minute data received for stock: {}", stockCode);
                return null;
            }

            log.info("Successfully fetched raw chart data for stock: {}", stockCode);
            return response;

        } catch (Exception e) {
            log.error("Error fetching raw minute chart data for stock: {}", stockCode, e);
            return null;
        }
    }

    /**
     * 한국투자증권 API에서 원본 응답 데이터 조회
     */
    private JsonNode fetchRawMinuteData(String stockCode) throws IOException {
        String url = buildMinuteChartUrl(stockCode);
        String accessToken = TokenManager.getAccessToken();

        String appKey = ConfigManager.get("app.key");
        String appSecret = ConfigManager.get("app.secret");

        log.debug("Requesting minute data from URL: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("authorization", "Bearer " + accessToken)
                .addHeader("appkey", appKey)
                .addHeader("appsecret", appSecret)
                .addHeader("tr_id", "FHKST03010200")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to fetch minute data. HTTP code: {}, message: {}",
                        response.code(), response.message());
                return null;
            }

            String responseBody = response.body().string();
            log.debug("Received response: {}", responseBody.substring(0, Math.min(200, responseBody.length())));

            // 원본 응답을 그대로 JsonNode로 반환
            return objectMapper.readTree(responseBody);
        }
    }

    /**
     * 분봉 차트 조회 URL 생성
     */
    private String buildMinuteChartUrl(String stockCode) {
        String baseUrl = "https://openapi.koreainvestment.com:9443/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice";
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String currentTime;
        // 주말이면 무조건 15:30:00
        if (today.getDayOfWeek().getValue() == 6 || today.getDayOfWeek().getValue() == 7) {
            currentTime = "153000";
        } else if (now.isAfter(LocalTime.of(15, 30, 0))) {
            currentTime = "153000";
        } else {
            currentTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        }

        return String.format("%s?FID_ETC_CLS_CODE=" +
                        "&FID_COND_MRKT_DIV_CODE=J" +
                        "&FID_INPUT_ISCD=%s" +
                        "&FID_INPUT_HOUR_1=%s" +
                        "&FID_PW_DATA_INCU_YN=Y",
                baseUrl, stockCode, currentTime);
    }
}
