package org.scoula.api.mocktrading;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.util.mocktrading.ConfigManager;

import java.io.IOException;

public class PriceApi {

    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";
    private static final String ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-price";

    private static final String APP_KEY = ConfigManager.get("app.key");
    private static final String APP_SECRET = ConfigManager.get("app.secret");
    private static final String TR_ID = "FHKST01010100"; // 실전투자 현재가 TR

    /**
     * 전체 응답 JsonNode 반환
     */
    public static JsonNode getPriceData(String code) throws IOException {
        String token = TokenManager.getAccessToken();

        HttpUrl url = HttpUrl.parse(BASE_URL + ENDPOINT).newBuilder()
                .addQueryParameter("FID_COND_MRKT_DIV_CODE", "J")  // KRX 시장
                .addQueryParameter("FID_INPUT_ISCD", code)         // 종목코드
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Bearer " + token)
                .addHeader("appkey", APP_KEY)
                .addHeader("appsecret", APP_SECRET)
                .addHeader("tr_id", TR_ID)
                .addHeader("custtype", "P") // 개인: P
                .build();

        OkHttpClient client = new OkHttpClient();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("\uD83D\uDD1B 현재가 조회 실패 (" + response.code() + ")\n응답: " + responseBody);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(responseBody);

            System.out.println("\uD83D\uDCE6 전체 응답 데이터:");
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));

            return json;
        }
    }
}