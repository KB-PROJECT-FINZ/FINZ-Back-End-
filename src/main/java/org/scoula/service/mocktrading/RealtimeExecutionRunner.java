package org.scoula.service.mocktrading;

import org.scoula.api.mocktrading.RealtimeExecutionClient;
import org.scoula.api.mocktrading.RealtimeBidsAndAsksClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class RealtimeExecutionRunner implements ApplicationListener<ContextRefreshedEvent> {

    private boolean alreadyStarted = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!alreadyStarted && event.getApplicationContext().getParent() == null) {
            alreadyStarted = true;
            System.out.println("🚀 Spring Context 초기화 완료 - WebSocket 클라이언트들 시작");

            try {
                 // 실시간 체결 클라이언트 시작
//                 RealtimeExecutionClient.startWebSocket();
//                 System.out.println("🚀 ⚡ 실시간 체결 WebSocket 클라이언트 실행됨");

                // 1초 대기 후 실시간 호가 클라이언트 시작
                // Thread.sleep(1000);
                RealtimeBidsAndAsksClient.startWebSocket();
                System.out.println("🚀 📊 실시간 호가 WebSocket 클라이언트 실행됨");

                System.out.println("🎯 두 클라이언트 모두 실행 완료 - 실시간 데이터 수신 대기 중...");

            } catch (Exception e) {
                System.err.println("❌ WebSocket 실행 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}