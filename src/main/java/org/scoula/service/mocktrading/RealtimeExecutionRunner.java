package org.scoula.service.mocktrading;

import org.scoula.api.mocktrading.RealtimeExecutionClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class RealtimeExecutionRunner implements ApplicationListener<ContextRefreshedEvent> {

    private boolean alreadyStarted = false;

    public RealtimeExecutionRunner() {
        System.out.println("🔧 RealtimeExecutionRunner 생성자 호출됨");
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Root Application Context에서만 실행 (중복 방지)
        if (!alreadyStarted && event.getApplicationContext().getParent() == null) {
            alreadyStarted = true;
            System.out.println("🚀 Spring Context 초기화 완료 - WebSocket 클라이언트 시작");

            try {
                RealtimeExecutionClient.startWebSocket();
                System.out.println("🚀 실시간 WebSocket 클라이언트 실행됨");
            } catch (Exception e) {
                System.err.println("❌ WebSocket 실행 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}