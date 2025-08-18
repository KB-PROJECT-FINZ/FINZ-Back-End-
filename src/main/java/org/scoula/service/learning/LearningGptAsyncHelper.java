package org.scoula.service.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.GptLearningContentResponseDto;
import org.scoula.domain.learning.vo.LearningContentVO;
import org.scoula.domain.learning.vo.LearningQuizVO;
import org.scoula.mapper.learning.LearningMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LearningGptAsyncHelper {

    private final LearningMapper learningMapper;
    private final GptContentService gptContentService;
    private final LearningSaveService learningSaveService;

    @Async
    public void asyncGenerateAndSaveContents(String groupCode, int count) {
        try {
            List<String> existingTitles = learningMapper.findTitlesByGroupCode(groupCode);
            int validCount = 0;
            int maxTry = count * 3; // 무한 루프 방지

            while (validCount < count && maxTry-- > 0) {
                GptLearningContentResponseDto gptDto = gptContentService.generateContent(groupCode, existingTitles);

                // 필터: body 유효성 체크
                if (gptDto.getBody() == null || gptDto.getBody().trim().isEmpty()) continue;
                if (gptDto.getQuizQuestion() == null || gptDto.getQuizAnswer() == null) continue;
                if (gptDto.getQuizQuestion().contains("?")) continue;

                // 콘텐츠 저장
                LearningContentVO newContent = convertGptResponseToVO(gptDto, groupCode);
                LearningQuizVO quiz = new LearningQuizVO();
                quiz.setQuestion(gptDto.getQuizQuestion());
                quiz.setAnswer(gptDto.getQuizAnswer().toUpperCase());
                quiz.setComment(gptDto.getQuizComment());
                quiz.setCreditReward(gptDto.getCreditReward());

                // 트랜잭션 저장: 둘 중 하나라도 실패하면 둘 다 롤백됨
                learningSaveService.saveContentAndQuiz(newContent, quiz);

                existingTitles.add(newContent.getTitle());
                validCount++;
            }

            System.out.println("GPT 콘텐츠 비동기 생성 완료 (생성된 콘텐츠 수: " + validCount + ")");
        } catch (Exception e) {
            System.err.println("[GPT 생성 에러] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LearningContentVO convertGptResponseToVO(GptLearningContentResponseDto dto, String groupCode) {
        LearningContentVO content = new LearningContentVO();
        content.setGroupCode(groupCode);
        content.setType("article");
        content.setTitle(dto.getTitle());
        content.setBody(dto.getBody());
        content.setImageUrl("");
        content.setYoutubeUrl("");
        content.setCreatedAt(LocalDateTime.now().toString());
        return content;
    }
}
