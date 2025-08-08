package org.scoula.service.chatbot.session;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.ChatSessionDto;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatBotMapper chatBotMapper;

    /** 세션 없으면 생성, intent 바뀌면 종료 후 새로 생성, 같으면 갱신 */
    public Integer ensureSession(Integer userId, Integer sessionId, IntentType currentIntent) {
        if (sessionId == null) {
            ChatSessionDto newSession = ChatSessionDto.builder()
                    .userId(userId)
                    .lastIntent(currentIntent)
                    .build();
            chatBotMapper.insertChatSession(newSession);
            log.info("[SESSION] 새 세션 생성 → id={}, intent={}", newSession.getId(), currentIntent);
            return newSession.getId();
        }

        IntentType lastIntent = chatBotMapper.getLastIntentBySessionId(sessionId);
        if (lastIntent == null || !currentIntent.equals(lastIntent)) {
            chatBotMapper.endChatSession(sessionId);
            log.info("[SESSION] intent 변경 → 기존 세션 종료: {}", sessionId);

            ChatSessionDto newSession = ChatSessionDto.builder()
                    .userId(userId)
                    .lastIntent(currentIntent)
                    .build();
            chatBotMapper.insertChatSession(newSession);
            log.info("[SESSION] 새 세션 생성 → id={}, intent={}", newSession.getId(), currentIntent);
            return newSession.getId();
        }

        // intent 동일 → 갱신만
        chatBotMapper.updateChatSessionIntent(ChatSessionDto.builder()
                .id(sessionId).lastIntent(currentIntent).build());
        log.info("[SESSION] intent 동일 → 세션 갱신: {}", sessionId);
        return sessionId;
    }

    /** 에러 발생 시 활성 세션 종료*/
    public void endActiveSessionIfAny(Integer userId) {
        Integer activeSessionId = chatBotMapper.getActiveSessionIdByUserId(userId);
        if (activeSessionId != null) {
            chatBotMapper.endChatSession(activeSessionId);
            log.info("❌ 에러로 세션 종료: {}", activeSessionId);
        }
    }
}
