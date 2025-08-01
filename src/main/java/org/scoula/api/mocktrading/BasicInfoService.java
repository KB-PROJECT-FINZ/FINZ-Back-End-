package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.scoula.util.mocktrading.ConfigManager;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicInfoService {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "https://openapi.koreainvestment.com:9443";
    private static final String ENDPOINT = "/uapi/domestic-stock/v1/quotations/search-stock-info";
    private static final String TR_ID = "CTPF1002R";

    public JsonNode getBasicInfo(String code) throws IOException {
        String token = TokenManager.getAccessToken();

        String appKey = ConfigManager.get("app.key");
        String appSecret = ConfigManager.get("app.secret");

        String url = BASE_URL + ENDPOINT + "?PRDT_TYPE_CD=300&PDNO=" + code;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("content-type", "application/json; charset=utf-8")
                .addHeader("authorization", "Bearer " + token)
                .addHeader("appkey", appKey)
                .addHeader("appsecret", appSecret)
                .addHeader("tr_id", TR_ID)
                .addHeader("custtype", "P")
                .addHeader("gt_uid", UUID.randomUUID().toString())
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                log.warn("❌ 응답 실패 - status: {}, body: {}", response.code(), responseBody);
                throw new IOException("📛 기본정보 조회 실패 (" + response.code() + ")\n응답: " + responseBody);
            }

            JsonNode json = objectMapper.readTree(responseBody);

//            log.info("기본정보 응답: \n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));

            return json;
        }
    }
}
