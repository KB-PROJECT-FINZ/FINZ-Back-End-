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
        body.put("max_tokens", 1800);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 3. GPT 요청
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(apiUrl, entity, JsonNode.class);

        // 4. 응답 파싱
        String content = response.getBody().get("choices").get(0).get("message").get("content").asText();

        String[] lines = content.split("\n");
        GptLearningContentResponseDto result = new GptLearningContentResponseDto();boolean isBody = false;
        StringBuilder bodyBuilder = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("title:")) {
                result.setTitle(line.replace("title:", "").trim());
                isBody = false;
            } else if (line.startsWith("body:")) {
                bodyBuilder.append(line.replace("body:", "").trim());
                isBody = true;
            } else if (line.startsWith("quiz_question:")) {
                result.setBody(bodyBuilder.toString());
                result.setQuizQuestion(line.replace("quiz_question:", "").trim());
                isBody = false;
            } else if (line.startsWith("quiz_answer:")) {
                result.setQuizAnswer(line.replace("quiz_answer:", "").trim());
            } else if (line.startsWith("quiz_comment:")) {
                result.setQuizComment(line.replace("quiz_comment:", "").trim());
            } else if (line.startsWith("credit_reward:")) {
                result.setCreditReward(Integer.parseInt(line.replace("credit_reward:", "").trim()));
            } else if (isBody) {
                bodyBuilder.append(" ").append(line.trim());
            }
        }
        return result;
    }
    private String buildPrompt(String groupCode, List<String> existingTitles) {
        return String.format("""
        당신은 투자 교육 콘텐츠 전문가입니다.
        아래의 사용자 성향에 맞는 새로운 투자 학습 콘텐츠를 작성해주세요.

        [사용자 성향 그룹]: %s
        [기존 콘텐츠 제목 목록]: %s

        조건:
        - 기존 콘텐츠와 중복되지 않는 새로운 주제를 선정할 것
        - 콘텐츠는 투자 초보자도 이해할 수 있도록 하되, 깊이 있고 전문적으로 작성할 것
        - 본문은 **줄글 형식**으로 작성하고, 줄마다 끊지 말고 자연스럽게 이어질 것
        - 최소 한 번 이상 본문 중간에 `\\n` 줄바꿈을 1~2개 삽입할 것
        - 전체 본문은 최소 600자 이상으로 작성할 것
        - 콘텐츠는 하나의 명확한 주제에 집중할 것

        출력 형식:
        title: (콘텐츠 제목)
        body: (줄글 형식으로 아티클이 이어짐. 문단 구분 시 \\n 사용)
        quiz_question: (OX 퀴즈 질문)
        quiz_answer: (O 또는 X)
        quiz_comment: (정답 해설)
        credit_reward: (정답 시 보상 포인트 수치, 예: 100)

        예시:
        title: ETF란 무엇인가 – 상장지수펀드의 개념과 활용법
        body: (ETF는 특정 지수의 수익률을 추종하는 상품으로...)\\n(이러한 ETF는 소액 투자자에게도 다양한 포트폴리오 구성 기회를 제공한다...)
        """, groupCode, String.join(", ", existingTitles));
    }

}
