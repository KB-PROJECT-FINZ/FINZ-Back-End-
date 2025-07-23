package org.scoula.mocktrading.external;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class VolumeRankingApi {

    @Autowired
    private TokenManager tokenManager;

    // ✅ 환경변수로 관리되는 설정값들
    @Value("${kis.api.app-key}")
    private String appKey;

    @Value("${kis.api.app-secret}")
    private String appSecret;

    @Value("${kis.api.base-url}")
    private String baseUrl;

    // ✅ API 호출 간격 및 제한
    private static final long API_CALL_INTERVAL_MS = 1000L;
    private static final int MAX_RETRY_COUNT = 3;
    private static final String VOLUME_RANKING_TR_ID = "FHPST01710000";

    /**
     * 거래량순위 조회 - 개선된 버전
     * @param marketType "J"(코스피), "Q"(코스닥)
     * @param limit 조회할 종목 수
     * @return 거래량 순위 리스트
     */
    public List<Map<String, Object>> getVolumeRanking(String marketType, int limit) throws IOException {
        log.info("🔍 거래량순위 조회 시작 - 시장: {}, 한도: {}",
                getMarketTypeName(marketType), limit);

        // ✅ 입력값 검증
        validateInputParameters(marketType, limit);

        // ✅ API 키 검증
        validateApiCredentials();

        String token = tokenManager.getAccessToken();

        HttpUrl url = buildVolumeRankingUrl(marketType);
        Request request = buildVolumeRankingRequest(url, token);

        // ✅ 재시도 로직과 함께 API 호출
        return executeVolumeRankingRequest(request, marketType, limit);
    }

    /**
     * 코스피와 코스닥 거래량 순위를 동시에 조회
     */
    public Map<String, List<Map<String, Object>>> getAllMarketVolumeRanking(int limit) throws IOException {
        log.info("🔍 전체 시장 거래량순위 조회 시작 - 한도: {}", limit);

        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        try {
            // 코스피 거래량 순위
            log.info("📊 코스피 거래량순위 조회");
            result.put("kospi", getVolumeRanking("J", limit));

            // API 호출 간격 준수
            waitForApiInterval();

            // 코스닥 거래량 순위
            log.info("📊 코스닥 거래량순위 조회");
            result.put("kosdaq", getVolumeRanking("Q", limit));

            log.info("✅ 전체 시장 거래량순위 조회 완료");
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ API 호출 중 인터럽트 발생");
            throw new IOException("API 호출 중 인터럽트 발생", e);
        } catch (IOException e) {
            log.error("❌ 전체 시장 거래량순위 조회 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 통합 거래량 순위 (코스피+코스닥 합쳐서 상위 N개)
     */
    public List<Map<String, Object>> getCombinedVolumeRanking(int limit) throws IOException {
        log.info("🔍 통합 거래량순위 조회 시작 - 한도: {}", limit);

        try {
            Map<String, List<Map<String, Object>>> allMarkets = getAllMarketVolumeRanking(limit);

            List<Map<String, Object>> combined = new ArrayList<>();

            // 두 시장의 데이터 합치기
            List<Map<String, Object>> kospiData = allMarkets.get("kospi");
            List<Map<String, Object>> kosdaqData = allMarkets.get("kosdaq");

            if (kospiData != null) combined.addAll(kospiData);
            if (kosdaqData != null) combined.addAll(kosdaqData);

            // 거래량 기준으로 정렬
            combined.sort((a, b) -> {
                Long volumeA = getLongValue(a, "tradingVolume");
                Long volumeB = getLongValue(b, "tradingVolume");
                return volumeB.compareTo(volumeA);
            });

            // 상위 limit 개수만 반환하고 순위 재조정
            List<Map<String, Object>> result = combined.subList(0, Math.min(limit, combined.size()));

            // 순위 재조정
            for (int i = 0; i < result.size(); i++) {
                result.get(i).put("rank", i + 1);
            }

            log.info("✅ 통합 거래량순위 조회 완료 - {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("❌ 통합 거래량순위 조회 실패: {}", e.getMessage());
            throw new IOException("통합 거래량순위 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 거래량순위 API URL 생성
     */
    private HttpUrl buildVolumeRankingUrl(String marketType) {
        return HttpUrl.parse(baseUrl + "/uapi/domestic-stock/v1/quotations/volume-rank")
                .newBuilder()
                .addQueryParameter("FID_COND_MRKT_DIV_CODE", marketType)
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
    }

    /**
     * 거래량순위 API 요청 생성
     */
    private Request buildVolumeRankingRequest(HttpUrl url, String token) {
        return new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("appkey", appKey)
                .addHeader("appsecret", appSecret)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("tr_id", VOLUME_RANKING_TR_ID)
                .addHeader("custtype", "P")
                .build();
    }

    /**
     * 재시도 로직이 있는 API 요청 실행
     */
    private List<Map<String, Object>> executeVolumeRankingRequest(Request request, String marketType, int limit) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    log.warn("⚠️ 거래량순위 조회 HTTP 실패 (시도 {}/{}): {} - {}",
                            retry + 1, MAX_RETRY_COUNT, response.code(), errorBody);

                    if (retry == MAX_RETRY_COUNT - 1) {
                        throw new IOException("거래량순위 조회 실패: " + response.code() + " - " + errorBody);
                    }
                    continue;
                }

                String responseBody = response.body().string();
                log.debug("📨 거래량순위 API 응답 받음: {}",
                        responseBody.substring(0, Math.min(200, responseBody.length())));

                return parseVolumeRankingResponse(responseBody, marketType, limit);

            } catch (IOException e) {
                log.warn("⚠️ 거래량순위 조회 실패 (시도 {}/{}): {}",
                        retry + 1, MAX_RETRY_COUNT, e.getMessage());

                if (retry == MAX_RETRY_COUNT - 1) {
                    throw e;
                }

                // 재시도 전 잠시 대기
                try {
                    Thread.sleep(1000 * (retry + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("재시도 중 인터럽트", ie);
                }
            }
        }

        return new ArrayList<>(); // 이 라인은 실행되지 않음
    }

    /**
     * API 응답 파싱
     */
    private List<Map<String, Object>> parseVolumeRankingResponse(String responseBody, String marketType, int limit) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(responseBody);

        String rtCd = json.path("rt_cd").asText();
        if (!"0".equals(rtCd)) {
            String errorMsg = json.path("msg1").asText();
            log.error("❌ 거래량순위 조회 API 오류: {}", errorMsg);
            throw new IOException("거래량순위 조회 API 오류: " + errorMsg);
        }

        JsonNode output = json.path("output");
        if (output.isMissingNode() || !output.isArray() || output.size() == 0) {
            log.warn("⚠️ 거래량순위 데이터가 없음 - 시장: {}", getMarketTypeName(marketType));
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = parseVolumeRankingData(output, limit);
        log.info("✅ 거래량순위 파싱 완료 - 시장: {}, {} 건", getMarketTypeName(marketType), result.size());

        return result;
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

            // 필수 필드 검증
            if (stockCode.isEmpty() || stockName.isEmpty() || "0".equals(currentPrice)) {
                log.debug("⚠️ 필수 데이터 누락으로 종목 스킵 - 코드: {}, 이름: {}", stockCode, stockName);
                continue;
            }

            // 종목명 길이 제한
            if (stockName.length() > 20) {
                stockName = stockName.substring(0, 20);
            }

            Map<String, Object> stockData = createStockData(
                    stockCode, stockName, currentPrice, change, changeRate,
                    changeSign, volume, tradingValue, count + 1
            );

            ranking.add(stockData);
            count++;
        }

        return ranking;
    }

    /**
     * 종목 데이터 객체 생성
     */
    private Map<String, Object> createStockData(String stockCode, String stockName,
                                                String currentPrice, String change, String changeRate, String changeSign,
                                                String volume, String tradingValue, int rank) {

        Map<String, Object> stockData = new HashMap<>();

        int price = parseInt(currentPrice);
        long vol = parseLong(volume);

        stockData.put("code", stockCode);
        stockData.put("name", stockName);
        stockData.put("currentPrice", price);
        stockData.put("change", parseInt(change));
        stockData.put("changePercent", parseDouble(changeRate));
        stockData.put("isPositive", isPositiveChange(changeSign));

        // 거래대금 계산 (거래량 * 현재가) 또는 API에서 제공하는 값 사용
        long calculatedTradingVolume = vol * price;
        long apiTradingVolume = parseLong(tradingValue);

        stockData.put("tradingVolume", apiTradingVolume > 0 ? apiTradingVolume : calculatedTradingVolume);
        stockData.put("volume", vol);
        stockData.put("rank", rank);

        log.debug("📊 종목 데이터 생성: {} - 현재가: {}, 거래량: {}, 거래대금: {}",
                stockName, price, vol, stockData.get("tradingVolume"));

        return stockData;
    }

    // ✅ 유틸리티 메서드들 (기존과 동일하지만 로깅 추가)

    private String getFieldValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.asText().isEmpty() && !"0".equals(field.asText())) {
                return field.asText();
            }
        }
        return "0";
    }

    private boolean isPositiveChange(String signCode) {
        if (signCode == null || signCode.isEmpty()) return false;
        return "1".equals(signCode) || "2".equals(signCode);
    }

    private int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.debug("⚠️ 정수 파싱 실패: '{}' -> 0", value);
            return 0;
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.debug("⚠️ 실수 파싱 실패: '{}' -> 0.0", value);
            return 0.0;
        }
    }

    private long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.debug("⚠️ Long 파싱 실패: '{}' -> 0", value);
            return 0L;
        }
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private String getMarketTypeName(String marketType) {
        return "J".equals(marketType) ? "코스피" : "Q".equals(marketType) ? "코스닥" : "알 수 없음";
    }

    private void waitForApiInterval() throws InterruptedException {
        log.debug("⏳ API 호출 간격 대기: {}ms", API_CALL_INTERVAL_MS);
        Thread.sleep(API_CALL_INTERVAL_MS);
    }

    private void validateInputParameters(String marketType, int limit) throws IOException {
        if (!"J".equals(marketType) && !"Q".equals(marketType)) {
            throw new IOException("잘못된 시장 타입: " + marketType + " (J=코스피, Q=코스닥)");
        }

        if (limit <= 0 || limit > 100) {
            throw new IOException("잘못된 조회 한도: " + limit + " (1-100 사이)");
        }
    }

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