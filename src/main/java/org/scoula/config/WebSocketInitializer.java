package org.scoula.config;

import org.scoula.controller.mocktrading.StockRelaySocket;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletContainerInitializer;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Set;

public class WebSocketInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {

        // sout 한글 깨짐 방지 설정
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ServerContainer serverContainer =
                (ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");

        if (serverContainer != null) {
            try {
                serverContainer.addEndpoint(StockRelaySocket.class);
                System.out.println("✅ WebSocket 엔드포인트 등록 성공: /ws/stock");
            } catch (Exception e) {
                System.err.println("❌ WebSocket 엔드포인트 등록 실패: " + e.getMessage());
            }
        } else {
            System.err.println("❌ WebSocket ServerContainer가 null입니다.");
        }
    }
}
