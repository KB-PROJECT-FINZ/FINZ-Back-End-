package org.scoula.service.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;
import org.scoula.api.mocktrading.*;
import org.scoula.api.mocktrading.TokenManager.TokenInfo;

public class Main {
    public static void main(String[] args) {
        try {
            // // 첫 번째 키 (MAIN)
             TokenInfo mainToken = TokenManager.getTokenInfo(TokenManager.TokenType.MAIN);
             System.out.println("✅ [MAIN] Access Token: " + mainToken.getAccessToken());
             System.out.println("✅ [MAIN] Approval Key: " + mainToken.getApprovalKey());
             System.out.println("✅ [MAIN] Expire Time (ms): " + mainToken.getExpireTime());

            // 두 번째 키 (SUB)
            TokenInfo subToken = TokenManager.getTokenInfo(TokenManager.TokenType.SUB);
            System.out.println("✅ [SUB] Access Token: " + subToken.getAccessToken());
            System.out.println("✅ [SUB] Approval Key: " + subToken.getApprovalKey());
            System.out.println("✅ [SUB] Expire Time (ms): " + subToken.getExpireTime());

            // 종목코드 기반 현재가 조회
//            JsonNode output = PriceApi.getPriceData("005930").path("output");
//            System.out.println(output);
            
            // 잔고 조회
//            BalanceApi.inquireBalance();

            // 분봉 조회
//            MinuteChartApi.getAndAggregateChart("005930", 5);  // 삼성전자 5분봉
            RealtimeNxtBidsAndAsksClient.startWebSocket("005930");
            RealtimeExecutionClient.startWebSocket("005930");


        } catch (Exception e) {
            System.err.println("❌ 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
