package org.scoula.controller.mocktrading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.scoula.domain.mocktrading.RealtimeBidsAndAsksDto;
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

    // 호가 데이터 브로드캐스트 메서드 추가
    public static void broadcastBidsAndAsks(RealtimeBidsAndAsksDto dto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "bidsAndAsks");  // 데이터 타입 구분
            response.set("data", mapper.valueToTree(dto));

            String jsonMessage = mapper.writeValueAsString(response);

            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(jsonMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 호가 브로드캐스트 오류: " + e.getMessage());
        }
    }

    // 기존 체결 데이터도 타입 구분 추가
    public static void broadcast(RealtimeStockDto dto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "execution");  // 데이터 타입 구분
            response.set("data", mapper.valueToTree(dto));

            String jsonMessage = mapper.writeValueAsString(response);

            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(jsonMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 체결 브로드캐스트 오류: " + e.getMessage());
        }
    }
}
