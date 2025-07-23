package org.scoula.mocktrading.external;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class MarketIndexApi {

    @Autowired
    private TokenManager tokenManager;

    private static final String APP_KEY = "PS4ZWuN1nszCbgArV5ZcccGwkvGiIwWo9533";
    private static final String APP_SECRET = "3Rs9bawB5FFzLFFa11naf0Jx4JyYqEESiPKR2sLTtJm3IogkA833HpBzqTnbOYGd+9AcLGVA2Z22V1oXXY4z4zug1tPxs7UU44fgSg3KzuYiwZ33qwAPqd4Cm3RohhjgNS2o9mNLAyev0mOL9QTwKTgfItpK7VgGYqE42VDo8tHwBb7pRAM=";
    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";

    /**
     * 시장지수 조회 - 데이터 반환용
     */
    public Map<String, Object> getMarketIndex(String indexCode) throws IOException {
        String token = tokenManager.getAccessToken();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/uapi/domestic-stock/v1/quotations/inquire-index-price")
                .newBuilder()
                .addQueryParameter("FID_COND_MRKT_DIV_CODE", "U")
                .addQueryParameter("FID_INPUT_ISCD", indexCode)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("appkey", APP_KEY)
                .addHeader("appsecret", APP_SECRET)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("tr_id", "FHPUP02100000")
                .addHeader("custtype", "P")
                .build();

        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            String responseBody = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(responseBody);

            String rtCd = json.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                throw new IOException("API 오류: " + json.path("msg1").asText());
            }

            JsonNode output = json.path("output");
            return parseIndexData(output, indexCode);

        } else {
            throw new IOException("HTTP 오류: " + response.code());
        }
    }

    /**
     * 코스피/코스닥 둘 다 조회
     */
    public Map<String, Object> getAllMarketIndices() throws IOException {
        Map<String, Object> result = new HashMap<>();

        try {
            result.put("kospi", getMarketIndex("0001"));
            Thread.sleep(1000);
            result.put("kosdaq", getMarketIndex("1001"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("API 호출 중 인터럽트", e);
        }

        return result;
    }

    private Map<String, Object> parseIndexData(JsonNode output, String indexCode) {
        Map<String, Object> result = new HashMap<>();

        JsonNode indexData = output.isArray() ? output.get(0) : output;

        String currentPrice = getFieldValue(indexData, "bstp_nmix_prpr", "stck_prpr", "current_price");
        String change = getFieldValue(indexData, "bstp_nmix_prdy_vrss", "prdy_vrss", "change");
        String changeRate = getFieldValue(indexData, "bstp_nmix_prdy_ctrt", "prdy_ctrt", "change_rate");
        String changeSign = getFieldValue(indexData, "prdy_vrss_sign", "sign", "change_sign");

        result.put("name", getIndexName(indexCode));
        result.put("value", parseDouble(currentPrice));
        result.put("change", parseDouble(change));
        result.put("changePercent", parseDouble(changeRate));
        result.put("isPositive", !"4".equals(changeSign) && !"5".equals(changeSign));

        return result;
    }

    private String getFieldValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.asText().isEmpty()) {
                return field.asText();
            }
        }
        return "0";
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String getIndexName(String indexCode) {
        return "0001".equals(indexCode) ? "KOSPI" : "KOSDAQ";
    }
}