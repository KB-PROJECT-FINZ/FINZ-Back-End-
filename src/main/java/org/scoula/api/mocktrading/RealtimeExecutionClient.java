package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.scoula.domain.mocktrading.RealtimeStockDto;
import org.scoula.controller.mocktrading.StockRelaySocket;
import java.net.URI;

// 한국투자증권 API 받아오는 역할
public class RealtimeExecutionClient {

    private static WebSocketClient client;
    private static final String WS_URL = "ws://ops.koreainvestment.com:21000/tryitout/H0STCNT0";

    public static void startWebSocket() throws Exception {
        String approvalKey = TokenManager.getTokenInfo().getApprovalKey();

        client = new WebSocketClient(new URI(WS_URL)) {
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
                    input.put("tr_key", "012450"); // 예: 한화에어로스페이스

                    ObjectNode body = mapper.createObjectNode();
                    body.set("input", input);

                    ObjectNode request = mapper.createObjectNode();
                    request.set("header", header);
                    request.set("body", body);

                    send(mapper.writeValueAsString(request));
                    System.out.println("▶ 구독 요청 전송 완료");
                } catch (Exception e) {
                    System.err.println("❌ 구독 요청 오류: " + e.getMessage());
                }
            }

            @Override
            public void onMessage(String message) {
                if (message.startsWith("0|H0STCNT0|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length < 4) return;

                    String[] fields = parts[3].split("\\^");
                    try {
                        RealtimeStockDto dto = new RealtimeStockDto(
                                fields[0], fields[1], fields[2],
                                fields[3].equals("2") || fields[3].equals("5") ? "-" + fields[3] : "+" + fields[3],
                                fields[4], fields[5],
                                fields[13], fields[14],
                                fields[10], fields[11]
                        );
                        System.out.println("📡 브로드캐스트 전송: " + dto);
                        StockRelaySocket.broadcast(dto);

                    } catch (Exception e) {
                        System.err.println("❌ 데이터 파싱 오류: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("❌ WebSocket 연결 종료: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("⚠ WebSocket 오류: " + ex.getMessage());
            }
        };

        client.connect();
    }

    public static void stopWebSocket() throws Exception {
        if (client != null) client.close();
    }
}
