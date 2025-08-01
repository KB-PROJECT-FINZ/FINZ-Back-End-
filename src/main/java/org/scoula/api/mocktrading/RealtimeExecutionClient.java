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
    private static final String WS_URL = "ws://ops.koreainvestment.com:21000/tryitout/H0UNCNT0";
    // 종목코드별 최초 1회만 메시지 출력
    private static final java.util.Set<String> startedStocks = new java.util.HashSet<>();

    public static void startWebSocket(String stockCode) throws Exception {
        TokenManager.TokenInfo subToken = TokenManager.getTokenInfo(TokenManager.TokenType.SUB);
        String approvalKey = subToken.getApprovalKey();
        System.out.println("실시간 체결에서 사용하는 키 " + approvalKey);

        // ✅ 호가 데이터와 동일한 패턴: 기존 연결이 있으면 먼저 종료
        if (client != null && client.isOpen()) {
            client.close();
            System.out.println("🔄 기존 체결 WebSocket 연결 종료 후 재연결");
        }

        client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshakeData) {
                System.out.println("✅ WebSocket 연결(H0UNCNT0) - 실시간 체결");

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode header = mapper.createObjectNode();
                    header.put("approval_key", approvalKey);
                    header.put("custtype", "P");
                    header.put("tr_type", "1");
                    header.put("content-type", "utf-8");

                    ObjectNode input = mapper.createObjectNode();
                    input.put("tr_id", "H0UNCNT0");
                    input.put("tr_key", stockCode); // 전달받은 stockCode 사용

                    ObjectNode body = mapper.createObjectNode();
                    body.set("input", input);

                    ObjectNode request = mapper.createObjectNode();
                    request.set("header", header);
                    request.set("body", body);

                    send(mapper.writeValueAsString(request));
                    System.out.println("▶ 실시간 체결 구독 요청 전송 완료 (" + stockCode + ")");
                } catch (Exception e) {
                    System.err.println("❌ 체결 구독 요청 오류: " + e.getMessage());
                }
            }

            @Override
            public void onMessage(String message) {
                if (message.startsWith("0|H0UNCNT0|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length < 4) {
                        System.err.println("❌ 체결 메시지 파트 부족: " + parts.length);
                        return;
                    }

                    String[] fields = parts[3].split("\\^");

                    // 필드 개수 확인
                    if (fields.length < 43) {
                        System.err.println("❌ 체결 필드 개수 부족: " + fields.length + " (최소 43개 필요)");
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
                        dto.setContractClassCode(getFieldSafely(fields, 21));  // CNTG_CLS_CODE
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
                            dto.setViStandardPrice(getFieldSafely(fields, 45));    // VL_STND_PRC
                        }

                        // 최초 1회만 "실시간 체결 데이터 정상 시작됨" 로그 출력
                        String stockCodeForMsg = dto.getStockCode();
                        if (!startedStocks.contains(stockCodeForMsg)) {
                            String currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            System.out.println("⚡ [" + currentTime + "] " + stockCodeForMsg + " 실시간 체결 데이터 정상 시작됨");
                            startedStocks.add(stockCodeForMsg);
                        }

                        // 실시간 체결 데이터 해석 및 출력
                        printRealtimeStockInfo(dto);

                        // WebSocket 브로드캐스트 (연결 상태 확인 후 전송)
                        try {
                            StockRelaySocket.broadcast(dto);
                        } catch (Exception broadcastException) {
                            System.err.println("❌ 체결 브로드캐스트 오류: " + broadcastException.getMessage());
                            // 브로드캐스트 실패해도 WebSocket 연결은 유지
                        }

                    } catch (Exception e) {
                        System.err.println("❌ 체결 데이터 파싱 오류: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (message.contains("H0UNCNT0")) {
                    // H0UNCNT0 관련 응답 메시지 (구독 완료 등)
                    System.out.println("⚡ 체결 응답: " + message);
                } else {
                    // 기타 메시지는 한 줄로만 표시 (호가 패턴과 동일)
                    System.out.println("⚡ 체결 기타: " + message.substring(0, Math.min(50, message.length())) + "...");
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                String initiator = remote ? "서버" : "클라이언트";
                String closeReason = getCloseCodeDescription(code);

                System.out.println("❌ 체결 WebSocket 연결 종료:");
                System.out.println("   └ 종료 코드: " + code + " (" + closeReason + ")");
                System.out.println("   └ 종료 사유: " + (reason.isEmpty() ? "사유 없음" : reason));
                System.out.println("   └ 종료 주체: " + initiator);

                // 비정상 종료인 경우 재연결 시도 (선택적)
                if (code != 1000 && code != 1001) {
                    System.out.println("   └ 비정상 종료 감지됨");
                    // 필요시 재연결 로직 추가 가능
                }
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("⚠ 체결 WebSocket 오류:");
                System.err.println("   └ 오류 타입: " + ex.getClass().getSimpleName());
                System.err.println("   └ 오류 메시지: " + ex.getMessage());

                // 상세 스택 트레이스는 필요시에만 출력
                if (ex.getMessage() != null && ex.getMessage().contains("Connection reset")) {
                    System.err.println("   └ 네트워크 연결이 재설정되었습니다.");
                } else if (ex.getMessage() != null && ex.getMessage().contains("timeout")) {
                    System.err.println("   └ 연결 시간이 초과되었습니다.");
                } else {
                    System.err.println("   └ 예상치 못한 오류입니다. 상세:");
                    ex.printStackTrace();
                }
            }
        };

        client.connect();
    }

    // ✅ 종목 변경 시 데이터 초기화 메서드 추가 (호가 패턴 적용)
    public static void clearStartedStocks() {
        startedStocks.clear();
        System.out.println("🔄 체결 데이터 시작 기록 초기화");
    }

    // 실시간 주식 정보를 해석하여 출력하는 메서드
    private static void printRealtimeStockInfo(RealtimeStockDto dto) {
        try {
            // 종목명 매핑 (필요시 확장 가능)
            String stockName = getStockName(dto.getStockCode());

            // 가격 정보 파싱
            int currentPrice = parseIntSafely(dto.getCurrentPrice());
            int prevDayDiff = parseIntSafely(dto.getPrevDayDiff());
            double prevDayRate = parseDoubleSafely(dto.getPrevDayRate());

            // 전일대비 부호 해석
            String trendSymbol = getPrevDayTrendSymbol(dto.getPrevDaySign());
            String trendDesc = dto.getPrevDaySignDescription();

            // 체결구분 해석
            String contractDesc = dto.getContractClassCodeDescription();

            // 시간 포맷팅
            String formattedTime = formatTime(dto.getContractTime());

            // 거래량 정보
            long contractVolume = parseLongSafely(dto.getContractVolume());
            long accumulatedVolume = parseLongSafely(dto.getAccumulatedVolume());

            // 핵심 정보 출력
//            System.out.println(String.format(
//                    "📈 [%s] %s(%s) %,d원 %s %+,d원(%.2f%%) | %s %,d주 체결 | 누적 %,d주",
//                    formattedTime,
//                    stockName,
//                    dto.getStockCode(),
//                    currentPrice,
//                    trendSymbol,
//                    prevDayDiff,
//                    prevDayRate,
//                    contractDesc,
//                    contractVolume,
//                    accumulatedVolume
//            ));

        } catch (Exception e) {
            System.err.println("❌ 체결 데이터 해석 오류: " + e.getMessage());
        }
    }

    // 종목코드로 종목명 반환 (확장 가능)
    private static String getStockName(String stockCode) {
        switch (stockCode) {
            case "000660": return "SK하이닉스";
            case "005930": return "삼성전자";
            case "000270": return "기아";
            case "207940": return "삼성바이오로직스";
            case "005380": return "현대차";
            case "051910": return "LG화학";
            case "006400": return "삼성SDI";
            case "035420": return "NAVER";
            case "003670": return "포스코퓨처엠";
            case "096770": return "SK이노베이션";
            // 필요한 종목 추가 가능
            default: return "종목명미상";
        }
    }

    // 전일대비 부호를 이모지로 변환
    private static String getPrevDayTrendSymbol(String sign) {
        switch (sign) {
            case "1": return "📈"; // 상한
            case "2": return "📈"; // 상승
            case "3": return "➡️"; // 보합
            case "4": return "📉"; // 하한
            case "5": return "📉"; // 하락
            default: return "❓";
        }
    }

    // 시간 포맷팅 (HHMMSS -> HH:MM:SS)
    private static String formatTime(String timeStr) {
        if (timeStr == null || timeStr.length() != 6) {
            return timeStr;
        }
        return String.format("%s:%s:%s",
                timeStr.substring(0, 2),
                timeStr.substring(2, 4),
                timeStr.substring(4, 6)
        );
    }

    // 안전한 정수 파싱
    private static int parseIntSafely(String str) {
        try {
            return str == null || str.isEmpty() ? 0 : Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 안전한 실수 파싱
    private static double parseDoubleSafely(String str) {
        try {
            return str == null || str.isEmpty() ? 0.0 : Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // 안전한 긴 정수 파싱
    private static long parseLongSafely(String str) {
        try {
            return str == null || str.isEmpty() ? 0L : Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // WebSocket 종료 코드 해석
    private static String getCloseCodeDescription(int code) {
        switch (code) {
            case 1000: return "정상 종료";
            case 1001: return "엔드포인트 종료 (페이지 이동 등)";
            case 1002: return "프로토콜 오류";
            case 1003: return "지원하지 않는 데이터 타입";
            case 1005: return "종료 코드 없음";
            case 1006: return "비정상 종료 (연결 끊김)";
            case 1007: return "잘못된 데이터 형식";
            case 1008: return "정책 위반";
            case 1009: return "메시지 크기 초과";
            case 1010: return "확장 협상 실패";
            case 1011: return "서버 내부 오류";
            case 1012: return "서비스 재시작";
            case 1013: return "나중에 다시 시도";
            case 1014: return "잘못된 게이트웨이";
            case 1015: return "TLS 핸드셰이크 실패";
            default: return "알 수 없는 종료 코드";
        }
    }

    // 연결 상태 확인 메서드 추가
    public static boolean isConnected() {
        return client != null && !client.isClosed();
    }

    // 연결 상태 출력 메서드
    public static void printConnectionStatus() {
        if (client == null) {
            System.out.println("🔌 WebSocket 상태: 초기화되지 않음");
        } else if (client.isOpen()) {
            System.out.println("✅ WebSocket 상태: 연결됨");
        } else if (client.isClosed()) {
            System.out.println("❌ WebSocket 상태: 연결 종료됨");
        } else {
            System.out.println("🔄 WebSocket 상태: 연결 중...");
        }
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
            System.out.println("🔌 체결 WebSocket 연결 종료");
        }
    }
}