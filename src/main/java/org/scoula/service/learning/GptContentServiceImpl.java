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
        - 기존 콘텐츠와 중복되지 않는 주제
        - 기존 콘텐츠와 다른 제목
        - 초보도 이해 가능하되 전문성 유지
        - 본문은 줄글(문단 구분 시 \\\\n 사용), 길이 약 600~800자
        - 반드시 아래 '출력 형식'만 출력. 추가 설명/마크다운/코드펜스 금지.
        
        OX 퀴즈 규칙(매우 중요):
            - OX는 **의문문이 아닌 '진술문'** 으로 작성 (물음표 금지, '?' 절대 사용하지 말 것)
            - 예) "ETF는 분산투자에 유리하다." (O/X 중 정답)
            
        출력 형식:
        title: (콘텐츠 제목)
        body: (줄글 본문. 문단 구분은 \\n)
        quiz_question: (OX 진술문, 물음표 금지)
        quiz_answer: (O 또는 X)
        quiz_comment: (정답 해설 1~2문장)
        credit_reward: (정수만, 예: 100)
        """, groupCode, String.join(", ", existingTitles));
    }

}
