package org.scoula.api.mocktrading;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.util.mocktrading.ConfigManager;

import java.io.IOException;
import java.util.*;

public class MultiPriceApi {

    private static final String BASE_URL = "https://openapi.koreainvestment.com:9443";
    private static final String ENDPOINT = "/uapi/domestic-stock/v1/quotations/intstock-multprice";

    private static final String APP_KEY = ConfigManager.get("app.key");
    private static final String APP_SECRET = ConfigManager.get("app.secret");
    private static final String TR_ID = "FHKST11300006";

    // 기본 종목코드 (30개 채우기 위한 더미 데이터)
    private static final String DEFAULT_STOCK_CODE = "005930";
    private static final String DEFAULT_MARKET_CODE = "J";

    /**
     * 다중 주식 가격 조회 (최대 30개)
     * 요청된 종목이 30개 미만인 경우 기본 종목으로 채움
     */
    public static JsonNode getMultiPriceData(List<String> stockCodes) throws IOException {
        if (stockCodes == null || stockCodes.isEmpty()) {
            throw new IllegalArgumentException("최소 1개의 종목코드가 필요합니다.");
        }

        if (stockCodes.size() > 30) {
            throw new IllegalArgumentException("최대 30개의 종목코드만 조회 가능합니다.");
        }

        String token = TokenManager.getAccessToken();

        // URL 빌더 생성
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + ENDPOINT).newBuilder();

        // 실제 요청 종목들 추가
        for (int i = 0; i < stockCodes.size(); i++) {
            int paramIndex = i + 1;
            urlBuilder.addQueryParameter("FID_COND_MRKT_DIV_CODE_" + paramIndex, DEFAULT_MARKET_CODE);
            urlBuilder.addQueryParameter("FID_INPUT_ISCD_" + paramIndex, stockCodes.get(i));
        }

        // 30개까지 기본값으로 채우기
        for (int i = stockCodes.size(); i < 30; i++) {
            int paramIndex = i + 1;
            urlBuilder.addQueryParameter("FID_COND_MRKT_DIV_CODE_" + paramIndex, DEFAULT_MARKET_CODE);
            urlBuilder.addQueryParameter("FID_INPUT_ISCD_" + paramIndex, DEFAULT_STOCK_CODE);
        }

        HttpUrl url = urlBuilder.build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("content-type", "application/json; charset=utf-8")
                .addHeader("authorization", "Bearer " + token)
                .addHeader("appkey", APP_KEY)
                .addHeader("appsecret", APP_SECRET)
                .addHeader("tr_id", TR_ID)
                .addHeader("custtype", "P")
                .build();

        OkHttpClient client = new OkHttpClient();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("🔛 다중 현재가 조회 실패 (" + response.code() + ")\n응답: " + responseBody);
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(responseBody);
        }
    }

    /**
     * API 응답에서 요청한 종목들만 필터링하여 반환
     */
    public static Map<String, JsonNode> filterRequestedStocks(JsonNode fullResponse, List<String> requestedCodes) {
        Map<String, JsonNode> filteredResult = new HashMap<>();

        if (fullResponse == null || !fullResponse.has("output")) {
            return filteredResult;
        }

        JsonNode outputArray = fullResponse.get("output");
        if (!outputArray.isArray()) {
            return filteredResult;
        }

        // 응답 데이터에서 요청한 종목코드만 추출
        Set<String> requestedCodesSet = new HashSet<>(requestedCodes);

        for (JsonNode stockData : outputArray) {
            if (stockData.has("inter_shrn_iscd")) {
                String stockCode = stockData.get("inter_shrn_iscd").asText();

                if (requestedCodesSet.contains(stockCode)) {
                    filteredResult.put(stockCode, stockData);
                }
            }
        }

        return filteredResult;
    }
}