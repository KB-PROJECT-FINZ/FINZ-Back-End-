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

            // WebSocket 자동 시작 로직 제거됨 - 이제 /api/chart/trading 엔드포인트를 통해 수동으로 시작
            System.out.println("💡 실시간 데이터는 /api/chart/trading?stockCode=종목코드 호출로 시작됩니다.");
        }
    }
}