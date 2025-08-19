package org.scoula.service.chatbot.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.ChatMessageDto;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.springframework.stereotype.Service;


/**
 * MessageService
 *
 * <p>챗봇 대화 메시지를 DB에 저장/관리하는 서비스.</p>
 *
 * <h3>전체 흐름</h3>
 * <ol>
 *   <li>사용자/시스템 메시지를 DTO로 빌드</li>
 *   <li>ChatBotMapper를 통해 DB에 insert</li>
 *   <li>MyBatis keyProperty="id" 매핑으로 message.id 자동 주입</li>
 *   <li>저장 로그 출력 및 ID 포함된 DTO 반환</li>
 * </ol>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>메시지 저장 로직을 서비스 단에서 일관되게 관리</li>
 *   <li>저장 직후 DB PK(id)를 포함한 DTO 반환</li>
 *   <li>로그 기록으로 추적성 확보</li>
 * </ul>
 *
 * <h3>예시</h3>
 * <pre>{@code
 * ChatMessageDto saved = messageService.save(
 *     1, 100, "user", "이 종목 분석해줘", IntentType.STOCK_ANALYZE
 * );
 * // saved.getId() → DB에 저장된 PK
 * }</pre>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class MessageService {
    private final ChatBotMapper chatBotMapper;

    /**
     * 메시지를 DB에 저장 후, ID가 포함된 ChatMessageDto 반환
     *
     * @param userId     사용자 ID
     * @param sessionId  세션 ID
     * @param role       메시지 역할(user/assistant/system)
     * @param content    메시지 내용
     * @param intentType 메시지 IntentType
     * @return DB에 저장된 메시지 DTO (ID 포함)
     */
    public ChatMessageDto save(Integer userId, Integer sessionId, String role, String content, IntentType intentType) {
        ChatMessageDto message = ChatMessageDto.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .intentType(intentType)
                .build();

        // DB insert (MyBatis → keyProperty="id"로 PK 자동 세팅)
        chatBotMapper.insertChatMessage(message); // insert 시 keyProperty="id"로 id 채워짐
        log.info("[MESSAGE] 저장 완료 → id={}", message.getId());
        return message; // ID 포함된 message 반환
    }
}
