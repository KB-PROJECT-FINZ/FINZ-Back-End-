package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.scoula.util.mocktrading.ConfigManager;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VariousChartApi {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 일/주/월/년별 차트 데이터 조회
     * @param stockCode 종목코드 (예: 005930)
     * @param periodCode 기간코드 (D:일, W:주, M:월, Y:년)
     * @param startDate 조회 시작일 (yyyyMMdd)
     * @param endDate 조회 종료일 (yyyyMMdd)
     */
    public JsonNode getChartData(String stockCode, String periodCode, String startDate, String endDate) {
        try {
            String url = buildChartUrl(stockCode, periodCode, startDate, endDate);
            String accessToken = TokenManager.getAccessToken();
            String appKey = ConfigManager.get("app.key");
            String appSecret = ConfigManager.get("app.secret");

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("authorization", "Bearer " + accessToken)
                    .addHeader("appkey", appKey)
                    .addHeader("appsecret", appSecret)
                    .addHeader("tr_id", "FHKST03010100")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to fetch chart data. HTTP code: {}, message: {}",
                            response.code(), response.message());
                    return null;
                }
                String responseBody = response.body().string();
                return objectMapper.readTree(responseBody);
            }
        } catch (Exception e) {
            log.error("Error fetching chart data for stock: {}, period: {}", stockCode, periodCode, e);
            return null;
        }
    }

    /**
     * 일/주/월/년별 차트 조회 URL 생성
     */
    private String buildChartUrl(String stockCode, String periodCode, String startDate, String endDate) {
        String baseUrl = "https://openapi.koreainvestment.com:9443/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
        // periodCode: D(일), W(주), M(월), Y(년)
        return String.format("%s?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=%s&FID_INPUT_DATE_1=%s&FID_INPUT_DATE_2=%s&FID_PERIOD_DIV_CODE=%s&FID_ORG_ADJ_PRC=0",
                baseUrl, stockCode, startDate, endDate, periodCode);
    }
}