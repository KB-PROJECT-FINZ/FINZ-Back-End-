package org.scoula.service.chatbot.session;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.ChatSessionDto;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.springframework.stereotype.Service;

/**
 * ChatSessionService
 *
 * <p>챗봇 대화 세션의 생명주기를 관리하는 서비스.</p>
 *
 * <h3>전체 흐름</h3>
 * <ol>
 *   <li>세션이 없으면 새 세션 생성</li>
 *   <li>Intent가 바뀌면 기존 세션 종료 후 새 세션 생성</li>
 *   <li>Intent가 같으면 기존 세션 갱신(keep-alive)</li>
 *   <li>에러 발생 시 활성 세션 강제 종료</li>
 * </ol>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>사용자 ID + IntentType 기반으로 세션 유효성 보장</li>
 *   <li>Intent 변경 시 context 전환을 위해 자동 세션 재생성</li>
 *   <li>DB 기반 세션 관리 → 중단/복구 가능</li>
 *   <li>로그로 세션 흐름 추적 가능</li>
 * </ul>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatBotMapper chatBotMapper;

    /**
     * 세션 보장 로직
     * <p>
     * - 세션 없음 → 새 세션 생성<br>
     * - Intent 변경 → 기존 세션 종료 후 새 세션 생성<br>
     * - Intent 동일 → 기존 세션 갱신
     * </p>
     *
     * @param userId       사용자 ID
     * @param sessionId    현재 세션 ID (null이면 없음)
     * @param currentIntent 현재 IntentType
     * @return 유효한 세션 ID
     */
    public Integer ensureSession(Integer userId, Integer sessionId, IntentType currentIntent) {

        // 1) 세션 없음 → 새 세션 생성
        if (sessionId == null) {
            ChatSessionDto newSession = ChatSessionDto.builder()
                    .userId(userId)
                    .lastIntent(currentIntent)
                    .build();
            chatBotMapper.insertChatSession(newSession);
            log.info("[SESSION] 새 세션 생성 → id={}, intent={}", newSession.getId(), currentIntent);
            return newSession.getId();
        }

        // 2) Intent 변경 → 기존 세션 종료 + 새 세션 생성
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

        // 3) Intent 동일 → 기존 세션 갱신
        chatBotMapper.updateChatSessionIntent(ChatSessionDto.builder()
                .id(sessionId).lastIntent(currentIntent).build());
        log.info("[SESSION] intent 동일 → 세션 갱신: {}", sessionId);
        return sessionId;
    }

    /**
     * 에러 발생 시 활성 세션 강제 종료
     *
     * @param userId 사용자 ID
     */
    public void endActiveSessionIfAny(Integer userId) {
        Integer activeSessionId = chatBotMapper.getActiveSessionIdByUserId(userId);
        if (activeSessionId != null) {
            chatBotMapper.endChatSession(activeSessionId);
            log.info("❌ 에러로 세션 종료: {}", activeSessionId);
        }
    }
}
