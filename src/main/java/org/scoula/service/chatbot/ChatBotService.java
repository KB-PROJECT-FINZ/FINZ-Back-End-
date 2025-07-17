package org.scoula.service.chatbot;


import org.scoula.domain.chatbot.dto.ChatRequestDto;
import org.scoula.domain.chatbot.dto.ChatResponseDto;
import org.springframework.stereotype.Service;


public interface ChatBotService {

    ChatResponseDto getChatResponse(ChatRequestDto request);
}
