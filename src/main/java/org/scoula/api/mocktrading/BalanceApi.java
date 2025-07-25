package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.scoula.util.mocktrading.ConfigManager;

import java.io.IOException;

public class BalanceApi {
    private static final String APP_SECRET = ConfigManager.get("app.secret");
    private static final String APP_KEY = ConfigManager.get("app.key");
//    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";
    private static final String BASE_URL = "https://openapi.koreainvestment.com:9443";

    // 예시 계좌번호
    private static final String CANO = ConfigManager.get("app.cano");       // 앞 8자리
    private static final String ACNT_PRDT_CD = "01";     // 뒤 2자리

    public static void inquireBalance() throws IOException {
        String token = TokenManager.getTokenInfo().getAccessToken();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/uapi/domestic-stock/v1/trading/inquire-balance")
                .newBuilder()
                .addQueryParameter("CANO", CANO)
                .addQueryParameter("ACNT_PRDT_CD", ACNT_PRDT_CD)
                .addQueryParameter("AFHR_FLPR_YN", "N")
                .addQueryParameter("OFL_YN", "N")
                .addQueryParameter("INQR_DVSN", "02")
                .addQueryParameter("UNPR_DVSN", "01")
                .addQueryParameter("FUND_STTL_ICLD_YN", "N")
                .addQueryParameter("FNCG_AMT_AUTO_RDPT_YN", "N")
                .addQueryParameter("PRCS_DVSN", "00")
                .addQueryParameter("CTX_AREA_FK100", "")
                .addQueryParameter("CTX_AREA_NK100", "")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("appkey", APP_KEY)
                .addHeader("appsecret", APP_SECRET)
                .addHeader("Content-Type", "application/json")
//                .addHeader("tr_id", "VTTC8434R")  // 모의투자 잔고조회용 TR
                .addHeader("tr_id", "TTTC8434R")  // 실전투자 잔고조회용 TR
                .build();

        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            String responseBody = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(responseBody, Object.class);
            String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            System.out.println("잔고 조회 결과:\n" + pretty);
        } else {
            System.err.println("잔고 조회 실패: " + response.code());
        }
    }
}

