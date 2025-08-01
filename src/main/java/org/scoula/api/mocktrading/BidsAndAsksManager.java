package org.scoula.api.mocktrading;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// 시간대별 호가 데이터 관리자
public class BidsAndAsksManager {

    private static volatile boolean isKrxActive = false;
    private static volatile boolean isNxtActive = false;
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final LocalTime KRX_CLOSE_TIME = LocalTime.of(15, 30); // 15:30

    public static void startBidsAndAsksService() {
        LocalTime currentTime = LocalTime.now();

        System.out.println("🕐 현재 시간: " + currentTime);
        System.out.println("🕐 KRX 마감 시간: " + KRX_CLOSE_TIME);

        if (currentTime.isBefore(KRX_CLOSE_TIME)) {
            // 15:30 이전 - KRX 호가 시작
            startKrxBidsAndAsks();
            // 15:30에 자동 전환 스케줄링
            scheduleNxtStart();
        } else {
            // 15:30 이후 - NXT 호가 시작
            startNxtBidsAndAsks();
        }
    }

    private static void startKrxBidsAndAsks() {
        try {
            if (!isKrxActive) {
                System.out.println("🚀 [KRX] 한국거래소 실시간 호가 시작");
                // RealtimeBidsAndAsksClient.startWebSocket(); // 자동 시작 제거됨 - ChartController에서 수동 호출
                isKrxActive = true;
            }
        } catch (Exception e) {
            System.err.println("❌ [KRX] 호가 시작 실패: " + e.getMessage());
        }
    }

    private static void startNxtBidsAndAsks() {
        try {
            if (!isNxtActive) {
                System.out.println("🚀 [NXT] 대체거래소 실시간 호가 시작");
                // RealtimeNxtBidsAndAsksClient.startWebSocket(); // 자동 시작 제거됨 - ChartController에서 수동 호출
                isNxtActive = true;
            }
        } catch (Exception e) {
            System.err.println("❌ [NXT] 호가 시작 실패: " + e.getMessage());
        }
    }

    private static void scheduleNxtStart() {
        LocalTime currentTime = LocalTime.now();
        LocalTime targetTime = KRX_CLOSE_TIME;

        // 만약 현재 시간이 이미 15:30을 넘었다면 다음날 15:30으로 설정
        if (currentTime.isAfter(targetTime)) {
            targetTime = targetTime.plusHours(24);
        }

        long delaySeconds = java.time.Duration.between(currentTime, targetTime).getSeconds();

        System.out.println("⏰ " + delaySeconds + "초 후 NXT 대체거래소로 자동 전환 예정");

        scheduler.schedule(() -> {
            try {
                System.out.println("🔄 15:30 도달 - KRX에서 NXT로 전환 시작");

                // KRX 호가 중지
                if (isKrxActive) {
                    RealtimeBidsAndAsksClient.stopWebSocket();
                    isKrxActive = false;
                    System.out.println("🛑 [KRX] 호가 중지됨");
                }

                // 잠시 대기
                Thread.sleep(2000);

                // NXT 호가 시작
                startNxtBidsAndAsks();

            } catch (Exception e) {
                System.err.println("❌ KRX -> NXT 전환 오류: " + e.getMessage());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    public static void stopAllBidsAndAsks() {
        try {
            if (isKrxActive) {
                RealtimeBidsAndAsksClient.stopWebSocket();
                isKrxActive = false;
            }
            if (isNxtActive) {
                RealtimeNxtBidsAndAsksClient.stopWebSocket();
                isNxtActive = false;
            }
            scheduler.shutdown();
            System.out.println("🛑 모든 호가 서비스 중지됨");
        } catch (Exception e) {
            System.err.println("❌ 호가 서비스 중지 오류: " + e.getMessage());
        }
    }

    public static boolean isKrxTime() {
        LocalTime currentTime = LocalTime.now();
        return currentTime.isBefore(KRX_CLOSE_TIME);
    }

    public static String getCurrentExchange() {
        return isKrxTime() ? "KRX" : "NXT";
    }
}