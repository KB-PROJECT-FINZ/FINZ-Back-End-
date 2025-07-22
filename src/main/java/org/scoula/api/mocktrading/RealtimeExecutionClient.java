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
                    
                    // 필드 개수 확인
                    if (fields.length < 43) {
                        System.err.println("❌ 필드 개수 부족: " + fields.length + " (최소 43개 필요)");
                        return;
                    }

                    try {
                        // 한투 API 필드를 DTO에 매핑
                        RealtimeStockDto dto = new RealtimeStockDto();
                        
                        // 기본 정보
                        dto.setStockCode(getFieldSafely(fields, 0));           // MKSC_SHRN_ISCD
                        dto.setContractTime(getFieldSafely(fields, 1));        // STCK_CNTG_HOUR
                        dto.setCurrentPrice(getFieldSafely(fields, 2));        // STCK_PRPR
                        dto.setPrevDaySign(getFieldSafely(fields, 3));         // PRDY_VRSS_SIGN
                        dto.setPrevDayDiff(getFieldSafely(fields, 4));         // PRDY_VRSS
                        dto.setPrevDayRate(getFieldSafely(fields, 5));         // PRDY_CTRT
                        dto.setWeightedAvgPrice(getFieldSafely(fields, 6));    // WGHN_AVRG_STCK_PRC
                        
                        // 시가/고가/저가
                        dto.setOpenPrice(getFieldSafely(fields, 7));           // STCK_OPRC
                        dto.setHighPrice(getFieldSafely(fields, 8));           // STCK_HGPR
                        dto.setLowPrice(getFieldSafely(fields, 9));            // STCK_LWPR
                        
                        // 호가 정보
                        dto.setAskPrice1(getFieldSafely(fields, 10));          // ASKP1
                        dto.setBidPrice1(getFieldSafely(fields, 11));          // BIDP1
                        
                        // 거래량 정보
                        dto.setContractVolume(getFieldSafely(fields, 12));     // CNTG_VOL
                        dto.setAccumulatedVolume(getFieldSafely(fields, 13));  // ACML_VOL
                        dto.setAccumulatedAmount(getFieldSafely(fields, 14));  // ACML_TR_PBMN
                        
                        // 체결 건수
                        dto.setSellContractCount(getFieldSafely(fields, 15));  // SELN_CNTG_CSNU
                        dto.setBuyContractCount(getFieldSafely(fields, 16));   // SHNU_CNTG_CSNU
                        dto.setNetBuyCount(getFieldSafely(fields, 17));        // NTBY_CNTG_CSNU
                        
                        // 체결 정보
                        dto.setContractIntensity(getFieldSafely(fields, 18));  // CTTR
                        dto.setTotalSellVolume(getFieldSafely(fields, 19));    // SELN_CNTG_SMTN
                        dto.setTotalBuyVolume(getFieldSafely(fields, 20));     // SHNU_CNTG_SMTN
                        dto.setContractType(getFieldSafely(fields, 21));       // CCLD_DVSN
                        dto.setBuyRate(getFieldSafely(fields, 22));            // SHNU_RATE
                        dto.setVolumeRate(getFieldSafely(fields, 23));         // PRDY_VOL_VRSS_ACML_VOL_RATE
                        
                        // 시간별 정보
                        dto.setOpenTime(getFieldSafely(fields, 24));           // OPRC_HOUR
                        dto.setOpenVsCurrentSign(getFieldSafely(fields, 25));  // OPRC_VRSS_PRPR_SIGN
                        dto.setOpenVsCurrentDiff(getFieldSafely(fields, 26));  // OPRC_VRSS_PRPR
                        dto.setHighTime(getFieldSafely(fields, 27));           // HGPR_HOUR
                        dto.setHighVsCurrentSign(getFieldSafely(fields, 28));  // HGPR_VRSS_PRPR_SIGN
                        dto.setHighVsCurrentDiff(getFieldSafely(fields, 29));  // HGPR_VRSS_PRPR
                        dto.setLowTime(getFieldSafely(fields, 30));            // LWPR_HOUR
                        dto.setLowVsCurrentSign(getFieldSafely(fields, 31));   // LWPR_VRSS_PRPR_SIGN
                        dto.setLowVsCurrentDiff(getFieldSafely(fields, 32));   // LWPR_VRSS_PRPR
                        
                        // 기타 정보
                        dto.setBusinessDate(getFieldSafely(fields, 33));       // BSOP_DATE
                        dto.setMarketOperationCode(getFieldSafely(fields, 34)); // NEW_MKOP_CLS_CODE
                        dto.setTradeHaltYn(getFieldSafely(fields, 35));        // TRHT_YN
                        dto.setAskRemainQty1(getFieldSafely(fields, 36));      // ASKP_RSQN1
                        dto.setBidRemainQty1(getFieldSafely(fields, 37));      // BIDP_RSQN1
                        dto.setTotalAskRemainQty(getFieldSafely(fields, 38));  // TOTAL_ASKP_RSQN
                        dto.setTotalBidRemainQty(getFieldSafely(fields, 39));  // TOTAL_BIDP_RSQN
                        dto.setVolumeTurnoverRate(getFieldSafely(fields, 40)); // VOL_TNRT
                        dto.setPrevSameTimeVolume(getFieldSafely(fields, 41)); // PRDY_SMNS_HOUR_ACML_VOL
                        dto.setPrevSameTimeRate(getFieldSafely(fields, 42));   // PRDY_SMNS_HOUR_ACML_VOL_RATE
                        
                        // 추가 필드들 (배열 길이가 허용하는 경우에만)
                        if (fields.length > 43) {
                            dto.setHourCode(getFieldSafely(fields, 43));           // HOUR_CLS_CODE
                        }
                        if (fields.length > 44) {
                            dto.setMarketCloseCode(getFieldSafely(fields, 44));    // MRKT_TRTM_CLS_CODE
                        }
                        if (fields.length > 45) {
                            dto.setViStandardPrice(getFieldSafely(fields, 45));    // VI_STND_PRC
                        }

                        System.out.println("📡 브로드캐스트 전송: " + dto.getStockCode() + " | " + 
                                         dto.getCurrentPrice() + " | " + dto.getContractTypeDescription() + 
                                         " | 체결량: " + dto.getContractVolume());
                        
                        StockRelaySocket.broadcast(dto);

                    } catch (Exception e) {
                        System.err.println("❌ 데이터 파싱 오류: " + e.getMessage());
                        e.printStackTrace();
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
            System.out.println("🔌 WebSocket 연결 종료");
        }
    }
}