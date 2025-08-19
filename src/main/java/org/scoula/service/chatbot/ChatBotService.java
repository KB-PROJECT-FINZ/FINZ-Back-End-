package org.scoula.service.chatbot;

import org.scoula.domain.chatbot.dto.ChatRequestDto;
import org.scoula.domain.chatbot.dto.ChatResponseDto;

/**
 * ChatBotService
 *
 * <p>챗봇 서비스의 최상위 진입점 인터페이스.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>사용자 요청(ChatRequestDto)을 받아 처리</li>
 *   <li>의도 분류(Intent) → 세션 관리 → 메시지 처리 → 응답 생성</li>
 *   <li>최종적으로 ChatResponseDto를 반환</li>
 * </ul>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>비즈니스 로직은 구현체(ChatBotServiceImpl)에서 담당</li>
 *   <li>컨트롤러 → 서비스 계층 간의 계약(Contract) 역할</li>
 *   <li>다른 구현체로 교체 가능 (테스트, 모의 구현 등)</li>
 * </ul>
 */
public interface ChatBotService {

    /**
     * 챗봇 응답 생성 메서드
     *
     * @param request 사용자 요청 DTO (userId, message, intent, sessionId 등 포함)
     * @return ChatResponseDto (최종 응답 메시지, 세션 정보 등)
     */
    ChatResponseDto getChatResponse(ChatRequestDto request);
}
