package org.scoula.service.chatbot.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.ChatMessageDto;
import org.scoula.domain.chatbot.enums.IntentType;
import org.scoula.mapper.chatbot.ChatBotMapper;
import org.springframework.stereotype.Service;


@Log4j2
@Service
@RequiredArgsConstructor
public class MessageService {
    private final ChatBotMapper chatBotMapper;

    // 메세지 저장 함수
    public ChatMessageDto save(Integer userId, Integer sessionId, String role, String content, IntentType intentType) {
        ChatMessageDto message = ChatMessageDto.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .intentType(intentType)
                .build();

        chatBotMapper.insertChatMessage(message); // insert 시 keyProperty="id"로 id 채워짐
        log.info("[MESSAGE] 저장 완료 → id={}", message.getId());
        return message; // ID 포함된 message 반환
    }
}
