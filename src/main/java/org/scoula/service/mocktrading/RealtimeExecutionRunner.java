package org.scoula.service.mocktrading;

import org.scoula.api.mocktrading.RealtimeExecutionClient;
import org.scoula.api.mocktrading.BidsAndAsksManager;
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
                // 시간대별 호가 서비스 시작 (KRX/NXT 자동 전환)
                BidsAndAsksManager.startBidsAndAsksService();

                // 체결 데이터는 필요시 주석 해제
                // RealtimeExecutionClient.startWebSocket();

            } catch (Exception e) {
                System.err.println("❌ WebSocket 실행 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}