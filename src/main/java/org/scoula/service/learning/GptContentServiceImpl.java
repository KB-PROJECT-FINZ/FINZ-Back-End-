package org.scoula.service.learning;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.scoula.domain.learning.dto.GptLearningContentResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GptContentServiceImpl implements GptContentService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public GptLearningContentResponseDto generateContent(String groupCode, List<String> existingTitles) {
        // 1. 프롬프트 생성
        String prompt = buildPrompt(groupCode, existingTitles);

        // 2. 요청 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 투자 교육 전문가입니다."),
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 3. GPT 요청
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(apiUrl, entity, JsonNode.class);

        // 4. 응답 파싱
        String content = response.getBody().get("choices").get(0).get("message").get("content").asText();

        // 예: "title: ETF란?\nbody: ETF는 ... 입니다." 형태
        String[] lines = content.split("\n", 2);
        String title = lines[0].replace("title:", "").trim();
        String bodyText = lines[1].replace("body:", "").trim();

        GptLearningContentResponseDto result = new GptLearningContentResponseDto();
        result.setTitle(title);
        result.setBody(bodyText);
        return result;
    }

    private String buildPrompt(String groupCode, List<String> existingTitles) {
        return String.format("""
                사용자 성향 그룹: %s
                기존에 생성된 콘텐츠 제목 목록: %s

                이 성향에 적합하면서도 새로운 주제를 가진 콘텐츠를 작성해주세요.
                아래 형식으로 답변해주세요:

                title: 콘텐츠 제목
                body: 콘텐츠 본문
                """, groupCode, String.join(", ", existingTitles));
    }
}
