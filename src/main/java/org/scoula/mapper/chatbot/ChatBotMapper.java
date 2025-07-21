package org.scoula.mapper.chatbot;

import org.mapstruct.Mapper;
import org.scoula.domain.chatbot.dto.ChatMessageDto;

import java.util.List;

@Mapper
public interface ChatBotMapper {
    void insertChatMessage(ChatMessageDto message);

    // 특정 세션에 해당하는 메세지 리스트로 조회
    List<ChatMessageDto> getMessagesBySessionId(Integer sessionId);

    // get
    ChatMessageDto getMessageById(Integer id);

}
