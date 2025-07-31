package org.scoula.service.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.GptLearningContentResponseDto;
import org.scoula.domain.learning.vo.LearningContentVO;
import org.scoula.domain.learning.vo.LearningQuizVO;
import org.scoula.mapper.LearningMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LearningGptAsyncHelper {

    private final LearningMapper learningMapper;
    private final GptContentService gptContentService;

    @Async
    public void asyncGenerateAndSaveContents(String groupCode, int count) {
        try {
            List<String> existingTitles = learningMapper.findTitlesByGroupCode(groupCode);

            for (int i = 0; i < count; i++) {
                GptLearningContentResponseDto gptDto = gptContentService.generateContent(groupCode, existingTitles);
                LearningContentVO newContent = convertGptResponseToVO(gptDto, groupCode);
                learningMapper.insertContent(newContent);

                LearningQuizVO quiz = new LearningQuizVO();
                quiz.setQuizId(newContent.getContentId());
                quiz.setQuestion(gptDto.getQuizQuestion());
                quiz.setAnswer(gptDto.getQuizAnswer());
                quiz.setComment(gptDto.getQuizComment());
                quiz.setCreditReward(gptDto.getCreditReward());
                learningMapper.insertQuiz(quiz);

                existingTitles.add(newContent.getTitle());
            }

            System.out.println("GPT 콘텐츠 비동기 생성 완료");
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
