package org.scoula.controller.chatbot;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.scoula.domain.chatbot.dto.ChatRequestDto;
import org.scoula.domain.chatbot.dto.ChatResponseDto;
import org.scoula.service.chatbot.ChatBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
@Api(tags = "AI 챗봇 API", description = "사용자와의 대화를 처리하는 AI 챗봇 기능을 제공합니다")
public class ChatBotController {

    private final ChatBotService chatBotService;

    @PostMapping
    @ApiOperation(value = "챗봇 대화", notes = "사용자의 질문에 대해 AI 챗봇이 응답을 생성합니다")
    @ApiResponses({
        @ApiResponse(code = 200, message = "성공적으로 응답을 생성했습니다"),
        @ApiResponse(code = 400, message = "잘못된 요청 데이터입니다"),
        @ApiResponse(code = 500, message = "서버 내부 오류가 발생했습니다")
    })
    public ResponseEntity<ChatResponseDto> chat(
            @ApiParam(value = "사용자 채팅 요청 정보", required = true)
            @RequestBody ChatRequestDto request) {
        ChatResponseDto response = chatBotService.getChatResponse(request);
        return ResponseEntity.ok(response);
    }

}
