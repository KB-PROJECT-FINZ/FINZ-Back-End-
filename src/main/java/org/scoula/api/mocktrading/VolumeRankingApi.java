package org.scoula.api.mocktrading;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.util.mocktrading.ConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class VolumeRankingApi {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private DataSource dataSource;

    private static final String APP_KEY = ConfigManager.get("app.key");
    private static final String APP_SECRET = ConfigManager.get("app.secret");

    @Value("${kis.api.base-url}")
    private String baseUrl;
    /**
     * 거래량순위 조회 - 탭 기능 지원 버전
     * @param marketType "J"(코스피), "Q"(코스닥)
     * @param limit 조회할 종목 수
     * @param blngClsCode 소속 구분 코드 (0:평균거래량, 1:거래증가율, 2:평균거래회전율, 3:거래금액순, 4:평균거래금액회전율)
     * @return 거래량 순위 리스트
     * @throws IOException
     */
    public List<Map<String, Object>> getVolumeRanking(String marketType, int limit, String blngClsCode) throws IOException {
        String token = tokenManager.getAccessToken();

        if (baseUrl == null) {
            log.error("❌ baseUrl이 주입되지 않았습니다. @Value 또는 @PropertySource 설정을 확인하세요.");
        }
        if (APP_KEY == null || APP_SECRET == null) {
            log.error("❌ app key 또는 secret이 null입니다. ConfigManager 설정 확인 필요.");
        }
        HttpUrl url = HttpUrl.parse(baseUrl + "/uapi/domestic-stock/v1/quotations/volume-rank")
                .newBuilder()
                .addQueryParameter("FID_COND_MRKT_DIV_CODE", marketType) // "J"(코스피), "Q"(코스닥)
                .addQueryParameter("FID_COND_SCR_DIV_CODE", "20171")
                .addQueryParameter("FID_INPUT_ISCD", "0000")
                .addQueryParameter("FID_DIV_CLS_CODE", "0")
                .addQueryParameter("FID_BLNG_CLS_CODE", blngClsCode) // 탭별 구분 코드
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

            return parseVolumeRankingData(output, limit, blngClsCode);

        } else {
            throw new IOException("거래량순위 조회 HTTP 오류: " + response.code());
        }
    }
    /**
     * 코스피와 코스닥 거래량 순위를 동시에 조회 - 탭 기능 지원
     * @param limit 각 시장별 조회할 종목 수
     * @param blngClsCode 소속 구분 코드
     * @return 전체 시장 거래량 순위
     * @throws IOException
     */
    public Map<String, List<Map<String, Object>>> getAllMarketVolumeRanking(int limit, String blngClsCode) throws IOException {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

            try {
            result.put("kospi", getVolumeRanking("J", limit, blngClsCode));
            Thread.sleep(1000);  // API 호출 간격
            result.put("kosdaq", getVolumeRanking("J", limit, blngClsCode));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("API 호출 중 인터럽트 발생", e);
        }

        return result;
    }

    /**
     * 통합 거래량 순위 (코스피+코스닥 합쳐서 상위 N개) - 탭 기능 지원
     * @param limit 조회할 종목 수
     * @param blngClsCode 소속 구분 코드
     * @return 통합 거래량 순위
     * @throws IOException
     */
    public List<Map<String, Object>> getCombinedVolumeRanking(int limit, String blngClsCode) throws IOException {
        Map<String, List<Map<String, Object>>> allMarkets = getAllMarketVolumeRanking(20, blngClsCode);

        List<Map<String, Object>> combined = new ArrayList<>();
        combined.addAll(allMarkets.get("kospi"));
        combined.addAll(allMarkets.get("kosdaq"));

        // 통합 랭킹 가져올 때 중복되게 가져오는 문제 해결
        Map<String, Map<String, Object>> deduplicated = new LinkedHashMap<>();
        for (Map<String, Object> stock : combined) {
            String code = (String) stock.get("code");
            if (!deduplicated.containsKey(code)) {
                deduplicated.put(code, stock);
            }
        }

        List<Map<String, Object>> uniqueStocks = new ArrayList<>(deduplicated.values());


        // 탭별 정렬 기준 적용
        combined.sort((a, b) -> {
            switch (blngClsCode) {
                case "0": // 평균거래량
                    Long volumeA = (Long) a.get("volume");
                    Long volumeB = (Long) b.get("volume");
                    return volumeB.compareTo(volumeA);

                case "1": // 거래증가율
                    Double rateA = (Double) a.get("volumeRate");
                    Double rateB = (Double) b.get("volumeRate");
                    return rateB.compareTo(rateA);

                case "2": // 평균거래회전율
                    Double turnoverA = (Double) a.get("turnoverRate");
                    Double turnoverB = (Double) b.get("turnoverRate");
                    return turnoverB.compareTo(turnoverA);

                case "3": // 거래금액순
                    Long tradingVolumeA = (Long) a.get("tradingVolume");
                    Long tradingVolumeB = (Long) b.get("tradingVolume");
                    return tradingVolumeB.compareTo(tradingVolumeA);

                case "4": // 평균거래금액회전율
                    Double amountTurnoverA = (Double) a.get("amountTurnoverRate");
                    Double amountTurnoverB = (Double) b.get("amountTurnoverRate");
                    return amountTurnoverB.compareTo(amountTurnoverA);

                default:
                    Long defaultA = (Long) a.get("tradingVolume");
                    Long defaultB = (Long) b.get("tradingVolume");
                    return defaultB.compareTo(defaultA);
            }
        });

        // 상위 limit 개수만 반환
        return combined.subList(0, Math.min(limit, combined.size()));
    }

    /**
     * JSON 응답 데이터를 파싱하여 거래량 순위 리스트로 변환 - 탭별 데이터 처리
     */
    private List<Map<String, Object>> parseVolumeRankingData(JsonNode output, int limit, String blngClsCode) {
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

            // 탭별 특화 데이터 추출
            String volumeRate = getFieldValue(stock, "prdy_vol_vrss_acml_vol_rate"); // 전일 대비 거래량 비율
            String turnoverRate = getFieldValue(stock, "vol_tnrt"); // 거래량 회전율
            String avgTradingValue = getFieldValue(stock, "avrg_vol_tr_pbmn"); // 평균 거래대금

            // 필수 필드가 없으면 스킵
            if (stockCode.isEmpty() || stockName.isEmpty()) {
                continue;
            }

            // 종목명이 너무 길면 잘라내기
            if (stockName.length() > 20) {
                stockName = stockName.substring(0, 20);
            }

            // DB에서 이미지 URL 조회
            String imageUrl = getStockImageUrl(stockCode);

            Map<String, Object> stockData = new HashMap<>();
            stockData.put("code", stockCode);
            stockData.put("name", stockName);
            stockData.put("currentPrice", parseInt(currentPrice));
            stockData.put("change", parseInt(change));
            stockData.put("changePercent", parseDouble(changeRate));
            stockData.put("isPositive", isPositiveChange(changeSign));
            stockData.put("volume", parseLong(volume)); // 거래량
            stockData.put("tradingVolume", parseLong(volume) * parseInt(currentPrice)); // 거래대금 계산
            stockData.put("rank", count + 1);
            stockData.put("imageUrl", imageUrl);
            stockData.put("rankingType", blngClsCode);

            // 탭별 추가 데이터
            stockData.put("volumeRate", parseDouble(volumeRate)); // 거래량 증가율
            stockData.put("turnoverRate", parseDouble(turnoverRate)); // 거래회전율
            stockData.put("avgTradingValue", parseLong(avgTradingValue)); // 평균 거래대금

            // 대금회전율 계산 (거래대금 / 시가총액 * 100)
            long marketCap = parseLong(volume) * parseInt(currentPrice) * 100; // 임시 계산
            double amountTurnoverRate = marketCap > 0 ?
                    (double)(parseLong(volume) * parseInt(currentPrice)) / marketCap * 100 : 0.0;
            stockData.put("amountTurnoverRate", amountTurnoverRate);

            ranking.add(stockData);
            count++;
        }

        return ranking;
    }

    /**
     * DB에서 종목 코드로 이미지 URL 조회
     */
    private String getStockImageUrl(String stockCode) {
        String imageUrl = null;
        String sql = "SELECT image_url FROM stocks WHERE code = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stockCode);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                imageUrl = rs.getString("image_url");
            }

        } catch (Exception e) {
            System.err.println("이미지 URL 조회 실패: " + stockCode + " - " + e.getMessage());
        }

        return imageUrl;
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