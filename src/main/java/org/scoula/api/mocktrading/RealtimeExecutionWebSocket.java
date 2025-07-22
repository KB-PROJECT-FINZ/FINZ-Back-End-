package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class RealtimeExecutionWebSocket {

    private static final String WS_URL = "ws://ops.koreainvestment.com:21000/tryitout/H0STCNT0"; // 모의투자 WebSocket URL

    public static void main(String[] args) throws Exception {
        TokenManager.TokenInfo tokenInfo = TokenManager.getTokenInfo();
        String approvalKey = tokenInfo.getApprovalKey();

        WebSocketClient client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshakeData) {
                System.out.println("✅ WebSocket 연결(H0STCNT0)");

                try {
                    ObjectMapper mapper = new ObjectMapper();

                    ObjectNode header = mapper.createObjectNode();
                    header.put("approval_key", approvalKey);
                    header.put("custtype", "P");
                    header.put("tr_type", "1");
                    header.put("content-type", "utf-8");

                    ObjectNode input = mapper.createObjectNode();
                    input.put("tr_id", "H0STCNT0");
                    input.put("tr_key", "005930"); // 삼성전자

                    ObjectNode body = mapper.createObjectNode();
                    body.set("input", input);

                    ObjectNode request = mapper.createObjectNode();
                    request.set("header", header);
                    request.set("body", body);

                    String json = mapper.writeValueAsString(request);
                    send(json);
                    System.out.println("▶ 구독 요청 전송:\n" + json);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(String message) {
                if (message.startsWith("0|H0STCNT0|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length < 4) {
                        System.out.println("⚠ 메시지 형식 오류: " + message);
                        return;
                    }

                    String data = parts[3];
                    String[] fields = data.split("\\^");

                    try {
                        String stockCode = fields[0];
                        String time = fields[1];
                        String price = fields[2];
                        String sign = fields[3];
                        String diff = fields[4];
                        String rate = fields[5];
                        String volume = fields[13];
                        String amount = fields[14];
                        String buyPrice = fields[11];
                        String sellPrice = fields[10];

                        String signSymbol = sign.equals("2") || sign.equals("5") ? "-" : "+";

                        System.out.println("📊 [실시간 체결 데이터]");
                        System.out.printf("📈 종목코드: %s%n", stockCode);
                        System.out.printf("🕒 체결시간: %s%n", time);
                        System.out.printf("💵 현재가: %s원 (%s %s, %s%%)%n", price, signSymbol, diff, rate);
                        System.out.printf("🧮 누적거래량: %s주, 누적거래대금: %s원%n", volume, amount);
                        System.out.printf("📉 매도호가: %s원, 📈 매수호가: %s원%n", sellPrice, buyPrice);
                        System.out.println("───────────────────────────────");

                        // TODO: 프론트엔드에 메시지 전송하는 코드 (예: WebSocket relay, REST endpoint로 전송 등)
                    } catch (Exception e) {
                        System.out.println("❌ 데이터 파싱 오류: " + e.getMessage());
                    }
                } else {
                    System.out.println("📩 [기타 메시지]:\n" + message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("❌ WebSocket 연결 종료(H0STCNT0): " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("⚠ WebSocket 오류(H0STCNT0): " + ex.getMessage());
            }
        };

        client.connect();
    }
}
