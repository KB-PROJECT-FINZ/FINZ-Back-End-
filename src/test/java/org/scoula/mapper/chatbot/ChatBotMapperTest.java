package org.scoula.mapper.chatbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.annotation.MapperScan;
import org.scoula.config.RootConfig;
import org.scoula.domain.chatbot.dto.ChatMessageDto;
import org.scoula.domain.chatbot.dto.ChatSessionDto;
import org.scoula.domain.chatbot.enums.IntentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class}) // 또는 필요하면 WebConfig, ServletConfig도 포함
class ChatBotMapperTest {


    @Autowired
    private ChatBotMapper chatBotMapper;

    @Test
    void insertChatMessage() {
        ChatMessageDto message = ChatMessageDto.builder()
                .userId(1)
                .sessionId(1)
                .role("user")
                .content("테스트 메시지입니다.")
                .intentType(IntentType.MESSAGE)
                .build();

        chatBotMapper.insertChatMessage(message);
    }

    @Test
    @Transactional
    void getMessagesBySessionId() {
        int testSessionId = 9999; // 겹치지 않게

        ChatMessageDto message = ChatMessageDto.builder()
                .userId(1)
                .sessionId(testSessionId)
                .role("user")
                .content("세션 테스트 메시지")
                .intentType(IntentType.MESSAGE)
                .build();

        chatBotMapper.insertChatMessage(message);

        List<ChatMessageDto> messages = chatBotMapper.getMessagesBySessionId(testSessionId);

        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("세션 테스트 메시지", messages.get(0).getContent());
    }


    @Test
    void getMessageById() {
        // 먼저 insert 하고 그 ID로 조회
        ChatMessageDto insertMessage = ChatMessageDto.builder()
                .userId(1)
                .sessionId(1)
                .role("user")
                .content("단건 조회 테스트 메시지")
                .intentType(IntentType.MESSAGE)
                .build();

        chatBotMapper.insertChatMessage(insertMessage);

        Integer insertedId = insertMessage.getId(); // insert 후 자동으로 id가 채워지는 구조라면

        assertNotNull(insertedId); // id가 null이면 실패

        ChatMessageDto result = chatBotMapper.getMessageById(insertedId);
        assertNotNull(result);
        assertEquals("단건 조회 테스트 메시지", result.getContent());
        assertEquals("user", result.getRole());
    }

    @Test
    @Transactional
    void chatSessionInsertUpdateTest() {
        // 1. 세션 insert
        ChatSessionDto session = ChatSessionDto.builder()
                .userId(1)
                .lastIntent(IntentType.RECOMMEND_PROFILE)
                .startedAt(LocalDateTime.now())
                .build();

        chatBotMapper.insertChatSession(session);
        assertNotNull(session.getId());

        Integer sessionId = session.getId();

        // 2. lastIntent update
        ChatSessionDto updatedSession = ChatSessionDto.builder()
                .id(sessionId)
                .lastIntent(IntentType.STOCK_ANALYZE)
                .build();

        chatBotMapper.updateChatSessionIntent(updatedSession);

        // 3. getLastIntentBySessionId
        IntentType lastIntent = chatBotMapper.getLastIntentBySessionId(sessionId);
        assertEquals(IntentType.STOCK_ANALYZE, lastIntent);

        // 4. endChatSession (종료시간 업데이트)
        chatBotMapper.endChatSession(sessionId);

        // 확인은 직접 select 쿼리 추가하거나 콘솔 로그로 확인 가능
    }
}