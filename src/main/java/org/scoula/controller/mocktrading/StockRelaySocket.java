package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.domain.mocktrading.RealtimeStockDto;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// 프론트엔드 중계 역할
@ServerEndpoint("/ws/stock")
public class StockRelaySocket {

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final ObjectMapper mapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("📡 프론트 WebSocket 연결됨: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("❌ 프론트 WebSocket 연결 종료됨: " + session.getId());
    }

    @OnMessage
    public void onMessage(String msg, Session session) {
        System.out.println("📩 클라이언트 메시지 수신: " + msg);
    }

    public static void broadcast(RealtimeStockDto dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            System.out.println("📤 브로드캐스트 JSON: " + json); // ← 추가
            for (Session session : sessions) {
                session.getBasicRemote().sendText(json);
            }
        } catch (IOException e) {
            System.err.println("📤 WebSocket 브로드캐스트 오류: " + e.getMessage());
        }
    }
}
