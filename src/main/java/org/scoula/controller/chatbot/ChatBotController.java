package org.scoula.controller.chatbot;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.dto.ChatRequestDto;
import org.scoula.domain.chatbot.dto.ChatResponseDto;
import org.scoula.service.chatbot.ChatBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(@RequestBody ChatRequestDto request) {
        ChatResponseDto response = chatBotService.getChatResponse(request);
        return ResponseEntity.ok(response);
    }

}
