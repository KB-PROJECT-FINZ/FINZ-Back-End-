package org.scoula.service.feedback;
import org.scoula.domain.journal.vo.InvestmentJournalVO;

import java.util.List;
import java.util.stream.Collectors;

public class GptPromptBuilder {
    public static String build(List<InvestmentJournalVO> journals) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 베테랑 투자 전문가이며, 모의투자를 바탕으로 개인 투자자에게 정밀한 피드백을 제공하는 역할입니다.\n");
        sb.append("다음 투자 일지를 참고하여, 사용자에게 다음과 같은 4가지 항목으로 피드백을 작성해주세요:\n");
        sb.append("1. 요약: 이번 주 총 작성한 일지 수, 빈번한 행동 유형(예: 매도/매수 타이밍, 관망 등) 분석, 전반적인 패턴이나 특이점 언급\n");
        sb.append("2. 강점: 투자 판단 중 타당하거나 잘한 부분을 칭찬해줄 것\n");
        sb.append("3. 개선점: 반복된 실수, 감정적 투자, 데이터 부족 등 개선할 부분 구체적으로 짚어줄 것\n");
        sb.append("4. 추천: 다음 주에 실천할 수 있는 구체적인 투자 습관/전략을 제시할 것 (예: 특정 상황에서의 대응법, 감정 기록법, 시장 분석법 등)\n\n");
        sb.append("피드백 형식은 아래와 같이 작성해주세요:\n");
        sb.append("요약: ...\n강점: ...\n개선점: ...\n추천: ...\n\n");
        sb.append("[일지 목록]\n");

        for (InvestmentJournalVO j : journals) {
            sb.append("날짜: ").append(j.getJournalDate()).append("\n");
            sb.append("감정: ").append(j.getEmotion()).append("\n");
            sb.append("이유: ").append(j.getReason()).append("\n");
            sb.append("실수: ").append(j.getMistake()).append("\n\n");
        }
        return sb.toString();
    }
}
