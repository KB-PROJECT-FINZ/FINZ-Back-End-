package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class MinuteChartApiKiwoom {

    /**
     * 키움증권 분봉 차트 데이터 조회 (컨트롤러에서 호출)
     */
    public JsonNode getKiwoomMinuteChartData(
            String stockCode
    ) {
        log.info("Fetching Kiwoom minute chart data for stock: {}", stockCode);

//        String accessToken = "xMCKZlGWKTZQ07eUmvMnof193KrnZ1Ho0RVX_eJAtC60PqLfSHR5Xguziw0g33zkDXrM_Q_NEGefm7M43bhNig";

        String accessToken;
        try {
            accessToken = TokenManager.getTokenInfo(TokenManager.TokenType.KIWOOM).getAccessToken();
        } catch (IOException e) {
            log.error("키움증권 토큰 발급 실패: {}", e.getMessage());
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode body = mapper.createObjectNode();
            body.put("stk_cd", stockCode);
            body.put("tic_scope", "1");
            body.put("upd_stkpc_tp", "0");

            OkHttpClient client = new OkHttpClient();
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                    .url("https://api.kiwoom.com/api/dostk/chart")
                    .addHeader("authorization", "Bearer " + accessToken)
                    .addHeader("api-id", "ka10080")
                    .addHeader("Content-Type", "application/json");

//            if (contYn != null) builder.addHeader("cont-yn", contYn);
//            if (nextKey != null) builder.addHeader("next-key", nextKey);

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
                return result;
            }
        } catch (Exception e) {
            log.error("Error fetching Kiwoom minute chart data for stock: {}", stockCode, e);
            return null;
        }
    }

}
