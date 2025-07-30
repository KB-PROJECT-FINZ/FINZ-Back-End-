package org.scoula.service.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.GptLearningContentResponseDto;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.vo.LearningContentVO;
import org.scoula.domain.learning.vo.LearningQuizVO;
import org.scoula.mapper.LearningMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningGptServiceImpl implements LearningGptService {

    private final LearningMapper learningMapper;
    private final GptContentService gptContentService;

    @Override
    public List<LearningContentDTO> recommendLearningContents(Long userId, int size) {
        String groupCode = learningMapper.findGroupCodeByUserId(userId);
        List<LearningContentVO> unreadContents = learningMapper.findUnreadContent(groupCode, userId);
        int remainToGenerate = size - unreadContents.size();

        if (remainToGenerate > 0) {
            // 기존 제목들 가져오기 → 중복 방지
            List<String> existingTitles = learningMapper.findTitlesByGroupCode(groupCode);

            for (int i = 0; i < remainToGenerate; i++) {
                // GPT로 콘텐츠 + 퀴즈 생성
                GptLearningContentResponseDto gptDto = gptContentService.generateContent(groupCode, existingTitles);

                // 콘텐츠 엔티티 생성
                LearningContentVO newContent = convertGptResponseToVO(gptDto, groupCode);

                // 1. 콘텐츠 DB 저장
                learningMapper.insertContent(newContent);

                // 2. 퀴즈 DB 저장
                LearningQuizVO quiz = new LearningQuizVO();
                quiz.setQuizId(newContent.getContentId()); // contentId를 퀴즈 ID로 사용
                quiz.setQuestion(gptDto.getQuizQuestion());
                quiz.setAnswer(gptDto.getQuizAnswer());
                quiz.setComment(gptDto.getQuizComment());
                quiz.setCreditReward(gptDto.getCreditReward());
                learningMapper.insertQuiz(quiz);

                // 3. 메모리에 추가
                unreadContents.add(newContent);
                existingTitles.add(newContent.getTitle());
            }
        }

        // 5. 최대 size만큼 잘라서 DTO로 변환 후 반환
        return unreadContents.stream()
                .limit(size)
                .map(LearningContentDTO::new)
                .toList();
    }

    private LearningContentVO convertGptResponseToVO(GptLearningContentResponseDto dto, String groupCode) {
        LearningContentVO content = new LearningContentVO();
        content.setGroupCode(groupCode);
        content.setType("article"); // 고정
        content.setTitle(dto.getTitle());
        content.setBody(dto.getBody());
        content.setImageUrl("");
        content.setYoutubeUrl("");
        content.setCreatedAt(LocalDateTime.now().toString());
        return content;
    }
}
