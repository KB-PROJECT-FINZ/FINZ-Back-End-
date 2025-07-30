package org.scoula.mapper.chatbot;

import org.apache.ibatis.annotations.Param;
import org.mapstruct.Mapper;
import org.scoula.domain.chatbot.dto.*;
import org.scoula.domain.chatbot.enums.IntentType;

import java.util.List;

@Mapper
public interface ChatBotMapper {
    void insertChatMessage(ChatMessageDto message);

    // 특정 세션에 해당하는 메세지 리스트로 조회
    List<ChatMessageDto> getMessagesBySessionId(Integer sessionId);

    // get
    ChatMessageDto getMessageById(Integer id);

    // -------- 세션 관련 Mapper ---------
    void insertChatSession(ChatSessionDto session);
    void updateChatSessionIntent(ChatSessionDto session);
    IntentType getLastIntentBySessionId(Integer sessionId);

    void endChatSession(@Param("sessionId") Integer sessionId);

    // 에러 저장
    void insertChatError(ChatErrorDto errorDto);

    // 종목 분석 내용 저장
    void insertAnalysis(ChatAnalysisDto dto);
    
    // 종목 추천 내용 저장
    void insertRecommendation(ChatRecommendationDto dto);


}
