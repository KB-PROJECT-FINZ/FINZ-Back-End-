package org.scoula.service.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import org.scoula.api.mocktrading.*;
import org.scoula.api.mocktrading.TokenManager.TokenInfo;

public class Main {
    public static void main(String[] args) {
        try {
            TokenInfo tokenInfo = TokenManager.getTokenInfo();
            System.out.println("✅ Access Token: " + tokenInfo.getAccessToken());
            System.out.println("✅ Approval Key: " + tokenInfo.getApprovalKey());
            System.out.println("✅ Expire Time (ms): " + tokenInfo.getExpireTime());

            // 종목코드 기반 현재가 조회
//            JsonNode output = PriceApi.getPriceData("005930").path("output");
//            System.out.println(output);
            
            // 잔고 조회
//            BalanceApi.inquireBalance();

            // 분봉 조회
//            MinuteChartApi.getAndAggregateChart("005930", 5);  // 삼성전자 5분봉

            RealtimeExecutionClient.startWebSocket();


        } catch (Exception e) {
            System.err.println("❌ 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
