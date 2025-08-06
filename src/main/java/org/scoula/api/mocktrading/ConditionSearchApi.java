package org.scoula.api.mocktrading;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.util.mocktrading.ConfigManager;

import java.io.IOException;

public class ConditionSearchApi {

    private static final String BASE_URL = "https://openapi.koreainvestment.com:9443";
    private static final String ENDPOINT = "/uapi/domestic-stock/v1/quotations/psearch-result";

    private static final String APP_KEY = ConfigManager.get("app.key2");
    private static final String APP_SECRET = ConfigManager.get("app.secret2");
    private static final String TR_ID = "HHKST03900400"; // 종목조건검색 목록조회 TR

    /**
     * 종목조건검색 목록조회 - 전체 응답 JsonNode 반환
     *
     * @param userId 사용자 ID
     * @param seq 종목조건검색 목록조회 API의 output인 'seq'을 이용 (0 부터 시작)
     * @return JsonNode 전체 응답 데이터
     * @throws IOException API 호출 실패 시
     */
    public static JsonNode getConditionSearchResult(String userId, String seq) throws IOException {
        TokenManager.TokenInfo subTokenInfo = TokenManager.getTokenInfo(TokenManager.TokenType.SUB);
        String token = subTokenInfo.getAccessToken();

        HttpUrl url = HttpUrl.parse(BASE_URL + ENDPOINT).newBuilder()
                .addQueryParameter("user_id", userId)
                .addQueryParameter("seq", seq)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("content-type", "application/json; charset=utf-8")
                .addHeader("authorization", "Bearer " + token)
                .addHeader("appkey", APP_KEY)
                .addHeader("appsecret", APP_SECRET)
                .addHeader("tr_id", TR_ID)
                .addHeader("custtype", "P") // 개인: P, 법인: B
                .build();

        OkHttpClient client = new OkHttpClient();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("🔛 종목조건검색 목록조회 실패 (" + response.code() + ")\n응답: " + responseBody);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(responseBody);

            return json;
        }
    }
}