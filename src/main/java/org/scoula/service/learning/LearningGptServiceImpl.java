package org.scoula.service.learning;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.GptLearningContentResponseDto;
import org.scoula.domain.learning.dto.LearningContentDTO;
import org.scoula.domain.learning.vo.LearningContentVO;
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
        // 1. 사용자 성향 groupCode 조회
        String groupCode = learningMapper.findGroupCodeByUserId(userId);

        // 2. 안 읽은 콘텐츠 조회
        List<LearningContentVO> unreadContents = learningMapper.findUnreadContent(groupCode, userId);

        // 3. 필요한 개수 계산
        int remainToGenerate = size - unreadContents.size();

        // 4. 부족할 경우 GPT로 콘텐츠 생성
        if (remainToGenerate > 0) {
            // 기존 제목들 가져오기 → 중복 방지
            List<String> existingTitles = learningMapper.findTitlesByGroupCode(groupCode);

            for (int i = 0; i < remainToGenerate; i++) {
                GptLearningContentResponseDto gptDto = gptContentService.generateContent(groupCode, existingTitles);
                LearningContentVO newContent = convertGptResponseToVO(gptDto, groupCode);

                // DB 저장
                learningMapper.insertContent(newContent);

                // 메모리에도 추가
                unreadContents.add(newContent);

                // 방금 생성한 제목도 기존 목록에 추가
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
