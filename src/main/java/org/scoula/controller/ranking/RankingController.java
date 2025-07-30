package org.scoula.controller.ranking;


import lombok.RequiredArgsConstructor;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.RankingByTraitGroupDto;
import org.scoula.service.ranking.RankingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/my")
    public MyRankingDto getMyRanking(HttpSession session,
                                     @RequestParam(required = false) String baseDate) {
        UserVo loginUser = (UserVo) session.getAttribute("loginUser");
        System.out.println("세션 사용자: " + loginUser);
        if (loginUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
        }

        try {
            Long userId = loginUser.getId().longValue();
            return rankingService.getMyRanking(userId, baseDate);
        } catch (Exception e) {
            System.out.println("🔥 /my 오류: " + e.getMessage());
            e.printStackTrace(); // 콘솔에 구체적인 오류 출력
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 실패");
        }
    }

    @GetMapping("/popular-stocks")
    public List<PopularStockDto> getTop5Stocks(@RequestParam String baseDate) {
        try {
            return rankingService.getTop5Stocks(baseDate);
        } catch (Exception e) {
            System.out.println("🔥 /popular-stocks 오류: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "인기 종목 조회 실패");
        }
    }

    @GetMapping("/weekly")
    public List<RankingByTraitGroupDto> getWeeklyRanking() {
        try {
            return rankingService.getWeeklyRanking();
        } catch (Exception e) {
            System.out.println("🔥 /weekly 오류: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "주간 랭킹 조회 실패");
        }
    }
    @GetMapping("/weekly/grouped")
    public Map<String, List<RankingByTraitGroupDto>> getGroupedRanking() {
        return rankingService.getGroupedWeeklyRanking();
    }
}