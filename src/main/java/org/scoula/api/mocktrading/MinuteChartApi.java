package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.scoula.util.mocktrading.CandleAggregator;
import org.scoula.util.mocktrading.ConfigManager;

import java.io.IOException;
import java.util.List;

public class MinuteChartApi {

    private static final String APP_KEY = ConfigManager.get("app.key");
    private static final String APP_SECRET = ConfigManager.get("app.secret");
    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";

    public static void getAndAggregateChart(String stockCode, int intervalMinutes) throws IOException {
        String token = TokenManager.getAccessToken();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice")
                .newBuilder()
                .addQueryParameter("FID_COND_MRKT_DIV_CODE", "J")       // 코스피
                .addQueryParameter("FID_INPUT_ISCD", stockCode)         // 종목 코드
                .addQueryParameter("FID_INPUT_HOUR_1", "113000")        // 몇 시까지 조회할지 (HHMMSS)
                .addQueryParameter("FID_PW_DATA_INCU_YN", "N")          // 체결 데이터 포함 여부
                .addQueryParameter("FID_ETC_CLS_CODE", "00")            // 기타 구분 코드
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("appkey", APP_KEY)
                .addHeader("appsecret", APP_SECRET)
                .addHeader("Content-Type", "application/json")
                .addHeader("tr_id", "FHKST03010200") // 분봉 조회 TR ID
                .build();

        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            String responseBody = response.body().string();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(responseBody);
            JsonNode output = json.path("output2");

            List<CandleAggregator.Candle> candles = CandleAggregator.aggregate(output, intervalMinutes);

            for (CandleAggregator.Candle candle : candles) {
                System.out.println(candle);
            }

        } else {
            System.err.println("분봉 조회 실패: " + response.code());
            System.err.println("응답 본문: " + response.body().string());
        }
    }
}
