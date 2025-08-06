package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class MinuteChartApiKiwoom {

    /**
     * 키움증권 분봉 차트 데이터 조회 (컨트롤러에서 호출)
     */

    public JsonNode getKiwoomMinuteChartData(String stockCode) {
        return getKiwoomMinuteChartData(stockCode, false);
    }

    public JsonNode getKiwoomMinuteChartData(String stockCode, boolean wrapWithStockCode) {
        log.info("Fetching Kiwoom minute chart data for stock: {}", stockCode);

        String accessToken;
        try {
            accessToken = TokenManager.getTokenInfo(TokenManager.TokenType.KIWOOM).getAccessToken();
        } catch (IOException e) {
            log.error("키움증권 토큰 발급 실패: {}", e.getMessage());
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();
            body.put("stk_cd", stockCode);
            body.put("tic_scope", "1");
            body.put("upd_stkpc_tp", "0");

            OkHttpClient client = new OkHttpClient();
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                    .url("https://api.kiwoom.com/api/dostk/chart")
                    .addHeader("authorization", "Bearer " + accessToken)
                    .addHeader("api-id", "ka10080")
                    .addHeader("Content-Type", "application/json");

            okhttp3.Request request = builder
                    .post(okhttp3.RequestBody.create(body.toString(), okhttp3.MediaType.parse("application/json")))
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Kiwoom API error: {}", response.message());
                    return null;
                }
                String responseBody = response.body().string();
                JsonNode result = mapper.readTree(responseBody);

                JsonNode chartArray = result.get("stk_min_pole_chart_qry");
                if (chartArray == null || !chartArray.isArray()) {
                    log.warn("분봉 데이터가 없습니다.");
                    return null;
                }

                String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // "yyyyMMdd"
                ArrayNode processedArray = mapper.createArrayNode();

                for (JsonNode candle : chartArray) {
                    String cntrTm = candle.path("cntr_tm").asText(); // "20250805133700"
                    if (cntrTm.length() != 14) continue;
                    String datePart = cntrTm.substring(0, 8);
                    if (!datePart.equals(today)) continue;

                    String hour = cntrTm.substring(8, 10);
                    String minute = cntrTm.substring(10, 12);
                    String timeKey = hour + minute + "00"; // "133700"

                    // 부호 제거
                    String stck_prpr = candle.path("cur_prc").asText().replaceAll("^[+-]", "");
                    String stck_oprc = candle.path("open_pric").asText().replaceAll("^[+-]", "");
                    String stck_hgpr = candle.path("high_pric").asText().replaceAll("^[+-]", "");
                    String stck_lwpr = candle.path("low_pric").asText().replaceAll("^[+-]", "");
                    String cntg_vol = candle.path("trde_qty").asText();

                    ObjectNode obj = mapper.createObjectNode();
                    obj.put("stck_cntg_hour", timeKey);
                    obj.put("stck_prpr", stck_prpr);
                    obj.put("stck_oprc", stck_oprc);
                    obj.put("stck_hgpr", stck_hgpr);
                    obj.put("stck_lwpr", stck_lwpr);
                    obj.put("cntg_vol", cntg_vol);

                    processedArray.add(obj);
                }

                if (wrapWithStockCode) {
                    ObjectNode wrapped = mapper.createObjectNode();
                    wrapped.put("stock_code", stockCode);
                    wrapped.set("data", processedArray);
                    return wrapped;
                } else {
                    return processedArray;
                }
            }
        } catch (Exception e) {
            log.error("Error fetching Kiwoom minute chart data for stock: {}", stockCode, e);
            return null;
        }
    }

}
