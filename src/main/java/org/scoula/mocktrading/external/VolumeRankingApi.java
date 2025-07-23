package org.scoula.mocktrading.external;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class VolumeRankingApi {

    @Autowired
    private TokenManager tokenManager;

    private static final String APP_KEY = "PS4ZWuN1nszCbgArV5ZcccGwkvGiIwWo9533";
    private static final String APP_SECRET = "3Rs9bawB5FFzLFFa11naf0Jx4JyYqEESiPKR2sLTtJm3IogkA833HpBzqTnbOYGd+9AcLGVA2Z22V1oXXY4z4zug1tPxs7UU44fgSg3KzuYiwZ33qwAPqd4Cm3RohhjgNS2o9mNLAyev0mOL9QTwKTgfItpK7VgGYqE42VDo8tHwBb7pRAM=";
    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";

    /**
     * 거래량순위 조회
     * @param marketType "J"(코스피), "Q"(코스닥)
     * @param limit 조회할 종목 수
     * @return 거래량 순위 리스트
     * @throws IOException
     */
    public List<Map<String, Object>> getVolumeRanking(String marketType, int limit) throws IOException {
        String token = tokenManager.getAccessToken();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/uapi/domestic-stock/v1/quotations/volume-rank")
                .newBuilder()
                .addQueryParameter("FID_COND_MRKT_DIV_CODE", marketType) // "J"(코스피), "Q"(코스닥)
                .addQueryParameter("FID_COND_SCR_DIV_CODE", "20171")
                .addQueryParameter("FID_INPUT_ISCD", "0000")
                .addQueryParameter("FID_DIV_CLS_CODE", "0")
                .addQueryParameter("FID_BLNG_CLS_CODE", "0")
                .addQueryParameter("FID_TRGT_CLS_CODE", "111111111")
                .addQueryParameter("FID_TRGT_EXLS_CLS_CODE", "0000000000")
                .addQueryParameter("FID_INPUT_PRICE_1", "")
                .addQueryParameter("FID_INPUT_PRICE_2", "")
                .addQueryParameter("FID_VOL_CNT", "")
                .addQueryParameter("FID_INPUT_DATE_1", "")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("appkey", APP_KEY)
                .addHeader("appsecret", APP_SECRET)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("tr_id", "FHPST01710000") // 거래량순위 조회용 TR ID
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
                String errorMsg = json.path("msg1").asText();
                throw new IOException("거래량순위 조회 API 오류: " + errorMsg);
            }

            JsonNode output = json.path("output");
            if (output.isMissingNode() || !output.isArray() || output.size() == 0) {
                return new ArrayList<>(); // 빈 리스트 반환
            }

            return parseVolumeRankingData(output, limit);

        } else {
            throw new IOException("거래량순위 조회 HTTP 오류: " + response.code());
        }
    }

    /**
     * 코스피와 코스닥 거래량 순위를 동시에 조회
     * @param limit 각 시장별 조회할 종목 수
     * @return 전체 시장 거래량 순위
     * @throws IOException
     */
    public Map<String, List<Map<String, Object>>> getAllMarketVolumeRanking(int limit) throws IOException {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        try {
            result.put("kospi", getVolumeRanking("J", limit));
            Thread.sleep(1000);  // API 호출 간격
            result.put("kosdaq", getVolumeRanking("Q", limit));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("API 호출 중 인터럽트 발생", e);
        }

        return result;
    }

    /**
     * 통합 거래량 순위 (코스피+코스닥 합쳐서 상위 N개)
     * @param limit 조회할 종목 수
     * @return 통합 거래량 순위
     * @throws IOException
     */
    public List<Map<String, Object>> getCombinedVolumeRanking(int limit) throws IOException {
        Map<String, List<Map<String, Object>>> allMarkets = getAllMarketVolumeRanking(20);

        List<Map<String, Object>> combined = new ArrayList<>();
        combined.addAll(allMarkets.get("kospi"));
        combined.addAll(allMarkets.get("kosdaq"));

        // 거래량 기준으로 정렬
        combined.sort((a, b) -> {
            Long volumeA = (Long) a.get("tradingVolume");
            Long volumeB = (Long) b.get("tradingVolume");
            return volumeB.compareTo(volumeA);
        });

        // 상위 limit 개수만 반환
        return combined.subList(0, Math.min(limit, combined.size()));
    }

    /**
     * JSON 응답 데이터를 파싱하여 거래량 순위 리스트로 변환
     */
    private List<Map<String, Object>> parseVolumeRankingData(JsonNode output, int limit) {
        List<Map<String, Object>> ranking = new ArrayList<>();

        int count = 0;
        for (JsonNode stock : output) {
            if (count >= limit) break;

            String stockCode = getFieldValue(stock, "mksc_shrn_iscd", "stck_shrn_iscd");
            String stockName = getFieldValue(stock, "hts_kor_isnm", "prdt_abrv_name");
            String currentPrice = getFieldValue(stock, "stck_prpr", "stck_clpr");
            String change = getFieldValue(stock, "prdy_vrss");
            String changeRate = getFieldValue(stock, "prdy_ctrt");
            String changeSign = getFieldValue(stock, "prdy_vrss_sign");
            String volume = getFieldValue(stock, "acml_vol");
            String tradingValue = getFieldValue(stock, "acml_tr_pbmn");

            // 필수 필드가 없으면 스킵
            if (stockCode.isEmpty() || stockName.isEmpty()) {
                continue;
            }

            // 종목명이 너무 길면 잘라내기
            if (stockName.length() > 20) {
                stockName = stockName.substring(0, 20);
            }

            Map<String, Object> stockData = new HashMap<>();
            stockData.put("code", stockCode);
            stockData.put("name", stockName);
            stockData.put("currentPrice", parseInt(currentPrice));
            stockData.put("change", parseInt(change));
            stockData.put("changePercent", parseDouble(changeRate));
            stockData.put("isPositive", isPositiveChange(changeSign));
            stockData.put("tradingVolume", parseLong(volume) * parseInt(currentPrice)); // 거래대금 계산
            stockData.put("volume", parseLong(volume)); // 거래량
            stockData.put("rank", count + 1);

            ranking.add(stockData);
            count++;
        }

        return ranking;
    }

    /**
     * 여러 필드명을 시도해서 값을 가져오는 헬퍼 메서드
     */
    private String getFieldValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.asText().isEmpty() && !"0".equals(field.asText())) {
                return field.asText();
            }
        }
        return "0";
    }

    /**
     * 등락 부호 판별
     */
    private boolean isPositiveChange(String signCode) {
        if (signCode == null || signCode.isEmpty()) return false;
        return "1".equals(signCode) || "2".equals(signCode);
    }

    /**
     * 문자열을 정수로 파싱
     */
    private int parseInt(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 문자열을 double로 파싱
     */
    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 문자열을 long으로 파싱
     */
    private long parseLong(String value) {
        if (value == null || value.isEmpty()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}