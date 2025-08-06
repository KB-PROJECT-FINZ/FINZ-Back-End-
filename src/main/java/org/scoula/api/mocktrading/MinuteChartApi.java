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
     * 09시부터 현재까지의 전체 분봉 차트 데이터 조회
     *
     * @param stockCode 종목코드
     * @return 병합된 분봉 차트 데이터
     */
    public JsonNode getFullDayMinuteChartData(String stockCode) {
        log.info("Fetching full day minute chart data for stock: {}", stockCode);

        try {
            // 시간 간격 계산 (현재부터 09:00까지 30분 간격)
            java.util.List<String> timeIntervals = calculateTimeIntervals();
            
            if (timeIntervals.isEmpty()) {
                log.warn("No time intervals calculated");
                return null;
            }

            log.info("Requesting data for {} time points: {}", timeIntervals.size(), timeIntervals);

            // 여러 API 요청을 순차적으로 처리
            java.util.List<JsonNode> responses = new java.util.ArrayList<>();
            
            for (String timePoint : timeIntervals) {
                try {
                    JsonNode response = fetchRawMinuteDataAtTime(stockCode, timePoint);
                    if (response != null) {
                        responses.add(response);
                        log.debug("Successfully fetched data for time: {}", timePoint);
                    }
                    // API 호출 간격을 위한 짧은 대기
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("Error fetching data for time {}: {}", timePoint, e.getMessage());
                }
            }

            if (responses.isEmpty()) {
                log.warn("No responses received for stock: {}", stockCode);
                return null;
            }

            // 응답 데이터 병합
            return mergeChartResponses(responses);

        } catch (Exception e) {
            log.error("Error fetching full day minute chart data for stock: {}", stockCode, e);
            return null;
        }
    }

    /**
     * 현재 시간부터 09:00까지 30분 간격으로 시간 포인트 계산
     */
    private java.util.List<String> calculateTimeIntervals() {
        java.util.List<String> intervals = new java.util.ArrayList<>();
        
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        // 주말이면 금요일 15:30만 반환
        if (today.getDayOfWeek().getValue() == 6 || today.getDayOfWeek().getValue() == 7) {
            intervals.add("153000");
            return intervals;
        }
        
        // 장 시작 전이면 빈 리스트 반환
        if (now.isBefore(LocalTime.of(9, 0))) {
            return intervals;
        }
        
        // 현재 시간 (장 마감 후면 15:30으로 제한)
        LocalTime endTime = now.isAfter(LocalTime.of(15, 30)) ? 
            LocalTime.of(15, 30) : now;
        
        // 현재 시간부터 역순으로 30분씩 계산
        LocalTime current = endTime;
        
        while (current.isAfter(LocalTime.of(9, 0)) || current.equals(LocalTime.of(9, 0))) {
            intervals.add(current.format(DateTimeFormatter.ofPattern("HHmm00")));
            current = current.minusMinutes(30);
        }
        
        log.info("Calculated {} time intervals: {}", intervals.size(), intervals);
        return intervals;
    }

    /**
     * 여러 차트 응답을 병합
     */
    private JsonNode mergeChartResponses(java.util.List<JsonNode> responses) {
        if (responses.isEmpty()) {
            return null;
        }

        // 첫 번째 응답을 기본 구조로 사용
        JsonNode baseResponse = responses.get(0);
        com.fasterxml.jackson.databind.node.ObjectNode result = 
            (com.fasterxml.jackson.databind.node.ObjectNode) baseResponse.deepCopy();

        // 모든 output2 데이터를 수집
        java.util.List<JsonNode> allData = new java.util.ArrayList<>();
        
        for (JsonNode response : responses) {
            JsonNode output2 = response.get("output2");
            if (output2 != null && output2.isArray()) {
                for (JsonNode item : output2) {
                    allData.add(item);
                }
            }
        }

        // 시간순 정렬 (stck_cntg_hour 기준)
        allData.sort((a, b) -> {
            String timeA = a.get("stck_cntg_hour").asText();
            String timeB = b.get("stck_cntg_hour").asText();
            return timeA.compareTo(timeB);
        });

        // 중복 제거 및 결과 설정
        java.util.Set<String> seenTimes = new java.util.HashSet<>();
        com.fasterxml.jackson.databind.node.ArrayNode mergedOutput2 = 
            objectMapper.createArrayNode();

        for (JsonNode item : allData) {
            String time = item.get("stck_cntg_hour").asText();
            if (!seenTimes.contains(time)) {
                seenTimes.add(time);
                mergedOutput2.add(item);
            }
        }

        result.set("output2", mergedOutput2);
        
        log.info("Merged {} responses into {} unique data points", 
            responses.size(), mergedOutput2.size());
        
        return result;
    }

    /**
     * 특정 시간에 대한 분봉 데이터 조회
     */
    private JsonNode fetchRawMinuteDataAtTime(String stockCode, String timePoint) throws IOException {
        String url = buildMinuteChartUrl(stockCode, timePoint);
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

            return objectMapper.readTree(responseBody);
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
                        "&FID_PW_DATA_INCU_YN=N",
                baseUrl, stockCode, currentTime);
    }

    /**
     * 특정 시간에 대한 분봉 차트 조회 URL 생성
     */
    private String buildMinuteChartUrl(String stockCode, String timePoint) {
        String baseUrl = "https://openapi.koreainvestment.com:9443/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice";
        
        return String.format("%s?FID_ETC_CLS_CODE=" +
                        "&FID_COND_MRKT_DIV_CODE=J" +
                        "&FID_INPUT_ISCD=%s" +
                        "&FID_INPUT_HOUR_1=%s" +
                        "&FID_PW_DATA_INCU_YN=N",
                baseUrl, stockCode, timePoint);
    }
}
