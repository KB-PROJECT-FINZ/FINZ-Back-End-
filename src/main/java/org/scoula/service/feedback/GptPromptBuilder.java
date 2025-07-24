package org.scoula.service.feedback;
import org.scoula.domain.journal.vo.InvestmentJournalVO;

import java.util.List;
import java.util.stream.Collectors;

public class GptPromptBuilder {
    public static String build(List<InvestmentJournalVO> journals) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 투자 전문가입니다. 모의투자를 진행하며 투자 일지를 작성한 이들에게 도움이 될만한 조언을 해줘야합니다. 이번 주 투자 일지를 바탕으로 피드백을 생성해주세요.\n");
        sb.append("형식: \n");
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
