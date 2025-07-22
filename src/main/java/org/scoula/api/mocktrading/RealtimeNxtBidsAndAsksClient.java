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

// NXT 대체거래소 실시간 호가 API 클라이언트 (15:30 이후)
public class RealtimeNxtBidsAndAsksClient {

    private static WebSocketClient client;
    private static final String WS_URL = "ws://ops.koreainvestment.com:31000/tryitout/H0NXASP0"; // NXT 전용 포트

    public static void startWebSocket() throws Exception {
        String approvalKey = TokenManager.getTokenInfo().getApprovalKey();

        client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshakeData) {
                System.out.println("✅ [NXT] WebSocket 연결(H0NXASP0) - 대체거래소 실시간 호가");

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode header = mapper.createObjectNode();
                    header.put("approval_key", approvalKey);
                    header.put("custtype", "P");
                    header.put("tr_type", "1");
                    header.put("content-type", "utf-8");

                    ObjectNode input = mapper.createObjectNode();
                    input.put("tr_id", "H0NXASP0"); // NXT 전용 TR_ID
                    input.put("tr_key", "005930"); // 삼성전자

                    ObjectNode body = mapper.createObjectNode();
                    body.set("input", input);

                    ObjectNode request = mapper.createObjectNode();
                    request.set("header", header);
                    request.set("body", body);

                    send(mapper.writeValueAsString(request));
                    System.out.println("▶ [NXT] 대체거래소 실시간 호가 구독 요청 전송 완료 (H0NXASP0)");
                } catch (Exception e) {
                    System.err.println("❌ [NXT] 호가 구독 요청 오류: " + e.getMessage());
                }
            }

            @Override
            public void onMessage(String message) {
                if (message.startsWith("0|H0NXASP0|")) { // NXT TR_ID로 수정
                    String[] parts = message.split("\\|");
                    if (parts.length < 4) {
                        System.err.println("❌ [NXT] 호가 메시지 파트 부족: " + parts.length);
                        return;
                    }

                    String[] fields = parts[3].split("\\^");

                    // NXT는 추가 필드가 있으므로 62개 이상 필요 (기존 58개 + NXT 전용 4개)
                    if (fields.length < 62) {
                        System.err.println("❌ [NXT] 호가 필드 개수 부족: " + fields.length + " (최소 62개 필요)");
                        return;
                    }

                    try {
                        // NXT API 필드를 DTO에 매핑
                        RealtimeBidsAndAsksDto dto = new RealtimeBidsAndAsksDto();

                        // 기본 정보
                        dto.setStockCode(getFieldSafely(fields, 0));        // MKSC_SHRN_ISCD
                        dto.setBusinessHour(getFieldSafely(fields, 1));     // BSOP_HOUR
                        dto.setHourCode(getFieldSafely(fields, 2));         // HOUR_CLS_CODE

                        // 매도호가 1~10 (String 타입으로 변경됨)
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

                        // 매수호가 1~10 (String 타입으로 변경됨)
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

                        // 매도잔량 1~10 (String 타입으로 변경됨)
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

                        // 매수잔량 1~10 (String 타입으로 변경됨)
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

                        // 총 잔량 정보 (String 타입으로 변경됨)
                        dto.setTotalAskQty(getFieldSafely(fields, 43));     // TOTAL_ASKP_RSQN
                        dto.setTotalBidQty(getFieldSafely(fields, 44));     // TOTAL_BIDP_RSQN
                        dto.setOvertimeAskQty(getFieldSafely(fields, 45));  // OVTM_TOTAL_ASKP_RSQN
                        dto.setOvertimeBidQty(getFieldSafely(fields, 46));  // OVTM_TOTAL_BIDP_RSQN

                        // 예상 체결 정보 (String 타입으로 변경됨)
                        dto.setExpectedPrice(getFieldSafely(fields, 47));   // ANTC_CNPR
                        dto.setExpectedQty(getFieldSafely(fields, 48));     // ANTC_CNQN
                        dto.setExpectedVolume(getFieldSafely(fields, 49));  // ANTC_VOL
                        dto.setExpectedDiff(getFieldSafely(fields, 50));    // ANTC_CNTG_VRSS
                        dto.setExpectedSign(getFieldSafely(fields, 51));    // ANTC_CNTG_VRSS_SIGN
                        dto.setExpectedRate(getFieldSafely(fields, 52));    // ANTC_CNTG_PRDY_CTRT

                        // 기타 정보 (String 타입으로 변경됨)
                        dto.setAccumulatedVolume(getFieldSafely(fields, 53)); // ACML_VOL
                        dto.setAskQtyChange(getFieldSafely(fields, 54));    // TOTAL_ASKP_RSQN_ICDC
                        dto.setBidQtyChange(getFieldSafely(fields, 55));    // TOTAL_BIDP_RSQN_ICDC
                        dto.setOvertimeAskChange(getFieldSafely(fields, 56)); // OVTM_TOTAL_ASKP_ICDC
                        dto.setOvertimeBidChange(getFieldSafely(fields, 57)); // OVTM_TOTAL_BIDP_ICDC

                        // NXT 전용 추가 필드들
                        String stockDealCode = getFieldSafely(fields, 58);  // STCK_DEAL_CLS_CODE
                        String krxMidPrice = getFieldSafely(fields, 59);    // KMID_PRC
                        String krxMidQty = getFieldSafely(fields, 60);      // KMID_TOTAL_RSQN
                        String krxMidCode = getFieldSafely(fields, 61);     // KMID_CLS_CODE

                        // 추가 NXT 필드가 있다면 계속
                        String nxtMidPrice = getFieldSafely(fields, 62);    // NMID_PRC
                        String nxtMidQty = getFieldSafely(fields, 63);      // NMID_TOTAL_RSQN
                        String nxtMidCode = getFieldSafely(fields, 64);     // NMID_CLS_CODE

                        // 📊 NXT 호가 정보 간단 출력 (매도1/매수1만)
                        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        System.out.println("📊 [NXT " + currentTime + "] " + dto.getStockCode() +
                                " | 매도1: " + dto.getAskPrice1() + "(" + dto.getAskQty1() + ")" +
                                " | 매수1: " + dto.getBidPrice1() + "(" + dto.getBidQty1() + ")");

                        // 호가 데이터 브로드캐스트 (기존과 동일한 DTO 사용)
                        StockRelaySocket.broadcastBidsAndAsks(dto);

                    } catch (Exception e) {
                        System.err.println("❌ [NXT] 호가 데이터 파싱 오류: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (message.contains("H0NXASP0")) { // NXT TR_ID로 변경
                    System.out.println("📊 [NXT] 호가 응답: " + message);
                } else {
                    System.out.println("📊 [NXT] 호가 기타: " + message.substring(0, Math.min(50, message.length())) + "...");
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("❌ [NXT] 호가 WebSocket 연결 종료: " + reason + " (코드: " + code + ")");
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("⚠ [NXT] 호가 WebSocket 오류: " + ex.getMessage());
            }
        };

        client.connect();
    }

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
            System.out.println("🔌 [NXT] 호가 WebSocket 연결 종료");
        }
    }
}