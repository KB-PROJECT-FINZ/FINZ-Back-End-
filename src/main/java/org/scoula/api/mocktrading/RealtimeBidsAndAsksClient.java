package org.scoula.api.mocktrading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.scoula.domain.mocktrading.RealtimeBidsAndAsksDto;
import org.scoula.controller.mocktrading.StockRelaySocket;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// 한국투자증권 실시간 호가 API 받아오는 역할
public class RealtimeBidsAndAsksClient {

    // 종목코드별 최초 1회만 메시지 출력
    private static final java.util.Set<String> startedStocks = new java.util.HashSet<>();

    private static WebSocketClient client;
    private static final String WS_URL = "ws://ops.koreainvestment.com:21000/tryitout/H0STASP0";

    public static void startWebSocket(String stockCode) throws Exception {
        String approvalKey = TokenManager.getTokenInfo().getApprovalKey();

         // 기존 연결이 있으면 먼저 종료
    if (client != null && client.isOpen()) {
        client.close();
        System.out.println("🔄 기존 WebSocket 연결 종료 후 재연결");
    }

        client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshakeData) {
                System.out.println("✅ WebSocket 연결(H0STASP0) - 실시간 호가");

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode header = mapper.createObjectNode();
                    header.put("approval_key", approvalKey);
                    header.put("custtype", "P");
                    header.put("tr_type", "1");
                    header.put("content-type", "utf-8");

                    ObjectNode input = mapper.createObjectNode();
                    input.put("tr_id", "H0STASP0");
                    input.put("tr_key", stockCode);

                    ObjectNode body = mapper.createObjectNode();
                    body.set("input", input);

                    ObjectNode request = mapper.createObjectNode();
                    request.set("header", header);
                    request.set("body", body);

                    send(mapper.writeValueAsString(request));
                    System.out.println("▶ 실시간 호가 구독 요청 전송 완료");
                } catch (Exception e) {
                    System.err.println("❌ 호가 구독 요청 오류: " + e.getMessage());
                }
            }

            @Override
            public void onMessage(String message) {
                if (message.startsWith("0|H0STASP0|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length < 4) {
                        System.err.println("❌ 호가 메시지 파트 부족: " + parts.length);
                        return;
                    }

                    String[] fields = parts[3].split("\\^");

                    // 필드 개수 확인 (최소 58개 필요)
                    if (fields.length < 58) {
                        System.err.println("❌ 호가 필드 개수 부족: " + fields.length + " (최소 58개 필요)");
                        return;
                    }

                    try {
                        // 한투 API 필드를 DTO에 매핑
                        RealtimeBidsAndAsksDto dto = new RealtimeBidsAndAsksDto();

                        // 기본 정보
                        dto.setStockCode(getFieldSafely(fields, 0));        // MKSC_SHRN_ISCD
                        dto.setBusinessHour(getFieldSafely(fields, 1));     // BSOP_HOUR
                        dto.setHourCode(getFieldSafely(fields, 2));         // HOUR_CLS_CODE

                        // 매도호가 1~10
                        dto.setAskPrice1(getFieldSafely(fields, 3));        // ASKP1
                        dto.setAskPrice2(getFieldSafely(fields, 4));        // ASKP2
                        dto.setAskPrice3(getFieldSafely(fields, 5));        // ASKP3
                        dto.setAskPrice4(getFieldSafely(fields, 6));        // ASKP4
                        dto.setAskPrice5(getFieldSafely(fields, 7));        // ASKP5
                        dto.setAskPrice6(getFieldSafely(fields, 8));        // ASKP6
                        dto.setAskPrice7(getFieldSafely(fields, 9));        // ASKP7
                        dto.setAskPrice8(getFieldSafely(fields, 10));       // ASKP8
                        dto.setAskPrice9(getFieldSafely(fields, 11));       // ASKP9
                        dto.setAskPrice10(getFieldSafely(fields, 12));      // ASKP10

                        // 매수호가 1~10
                        dto.setBidPrice1(getFieldSafely(fields, 13));       // BIDP1
                        dto.setBidPrice2(getFieldSafely(fields, 14));       // BIDP2
                        dto.setBidPrice3(getFieldSafely(fields, 15));       // BIDP3
                        dto.setBidPrice4(getFieldSafely(fields, 16));       // BIDP4
                        dto.setBidPrice5(getFieldSafely(fields, 17));       // BIDP5
                        dto.setBidPrice6(getFieldSafely(fields, 18));       // BIDP6
                        dto.setBidPrice7(getFieldSafely(fields, 19));       // BIDP7
                        dto.setBidPrice8(getFieldSafely(fields, 20));       // BIDP8
                        dto.setBidPrice9(getFieldSafely(fields, 21));       // BIDP9
                        dto.setBidPrice10(getFieldSafely(fields, 22));      // BIDP10

                        // 매도잔량 1~10
                        dto.setAskQty1(getFieldSafely(fields, 23));         // ASKP_RSQN1
                        dto.setAskQty2(getFieldSafely(fields, 24));         // ASKP_RSQN2
                        dto.setAskQty3(getFieldSafely(fields, 25));         // ASKP_RSQN3
                        dto.setAskQty4(getFieldSafely(fields, 26));         // ASKP_RSQN4
                        dto.setAskQty5(getFieldSafely(fields, 27));         // ASKP_RSQN5
                        dto.setAskQty6(getFieldSafely(fields, 28));         // ASKP_RSQN6
                        dto.setAskQty7(getFieldSafely(fields, 29));         // ASKP_RSQN7
                        dto.setAskQty8(getFieldSafely(fields, 30));         // ASKP_RSQN8
                        dto.setAskQty9(getFieldSafely(fields, 31));         // ASKP_RSQN9
                        dto.setAskQty10(getFieldSafely(fields, 32));        // ASKP_RSQN10

                        // 매수잔량 1~10
                        dto.setBidQty1(getFieldSafely(fields, 33));         // BIDP_RSQN1
                        dto.setBidQty2(getFieldSafely(fields, 34));         // BIDP_RSQN2
                        dto.setBidQty3(getFieldSafely(fields, 35));         // BIDP_RSQN3
                        dto.setBidQty4(getFieldSafely(fields, 36));         // BIDP_RSQN4
                        dto.setBidQty5(getFieldSafely(fields, 37));         // BIDP_RSQN5
                        dto.setBidQty6(getFieldSafely(fields, 38));         // BIDP_RSQN6
                        dto.setBidQty7(getFieldSafely(fields, 39));         // BIDP_RSQN7
                        dto.setBidQty8(getFieldSafely(fields, 40));         // BIDP_RSQN8
                        dto.setBidQty9(getFieldSafely(fields, 41));         // BIDP_RSQN9
                        dto.setBidQty10(getFieldSafely(fields, 42));        // BIDP_RSQN10

                        // 총 잔량 정보
                        dto.setTotalAskQty(getFieldSafely(fields, 43));     // TOTAL_ASKP_RSQN
                        dto.setTotalBidQty(getFieldSafely(fields, 44));     // TOTAL_BIDP_RSQN
                        dto.setOvertimeAskQty(getFieldSafely(fields, 45));  // OVTM_TOTAL_ASKP_RSQN
                        dto.setOvertimeBidQty(getFieldSafely(fields, 46));  // OVTM_TOTAL_BIDP_RSQN

                        // 예상 체결 정보
                        dto.setExpectedPrice(getFieldSafely(fields, 47));   // ANTC_CNPR
                        dto.setExpectedQty(getFieldSafely(fields, 48));     // ANTC_CNQN
                        dto.setExpectedVolume(getFieldSafely(fields, 49));  // ANTC_VOL
                        dto.setExpectedDiff(getFieldSafely(fields, 50));    // ANTC_CNTG_VRSS
                        dto.setExpectedSign(getFieldSafely(fields, 51));    // ANTC_CNTG_VRSS_SIGN
                        dto.setExpectedRate(getFieldSafely(fields, 52));    // ANTC_CNTG_PRDY_CTRT

                        // 기타 정보
                        dto.setAccumulatedVolume(getFieldSafely(fields, 53)); // ACML_VOL
                        dto.setAskQtyChange(getFieldSafely(fields, 54));    // TOTAL_ASKP_RSQN_ICDC
                        dto.setBidQtyChange(getFieldSafely(fields, 55));    // TOTAL_BIDP_RSQN_ICDC
                        dto.setOvertimeAskChange(getFieldSafely(fields, 56)); // OVTM_TOTAL_ASKP_ICDC
                        dto.setOvertimeBidChange(getFieldSafely(fields, 57)); // OVTM_TOTAL_BIDP_ICDC

                        // 📊 간단한 호가 정보 요약 출력

                        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        String stockCodeForMsg = dto.getStockCode();
                        if (!startedStocks.contains(stockCodeForMsg)) {
                            System.out.println("📊 [" + currentTime + "]" + stockCodeForMsg + " 호가 정상 시작됨");
                            startedStocks.add(stockCodeForMsg);
                        }

                        // 호가 데이터 브로드캐스트
                        StockRelaySocket.broadcastBidsAndAsks(dto);

                    } catch (Exception e) {
                        System.err.println("❌ 호가 데이터 파싱 오류: " + e.getMessage());
                    }
                } else if (message.contains("H0STASP0")) {
                    // H0STASP0 관련 응답 메시지 (구독 완료 등)
                    System.out.println("📊 호가 응답: " + message);
                } else {
                    // 기타 메시지는 한 줄로만 표시
                    System.out.println("📊 호가 기타: " + message.substring(0, Math.min(50, message.length())) + "...");
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("❌ 호가 WebSocket 연결 종료: " + reason + " (코드: " + code + ")");
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("⚠ 호가 WebSocket 오류: " + ex.getMessage());
            }
        };

        client.connect();
    }

    // 안전한 필드 접근을 위한 헬퍼 메서드
    private static String getFieldSafely(String[] fields, int index) {
        try {
            return (index < fields.length && fields[index] != null) ? fields[index] : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static void stopWebSocket() throws Exception {
        if (client != null) {
            client.close();
            System.out.println("🔌 호가 WebSocket 연결 종료");
        }
    }
}