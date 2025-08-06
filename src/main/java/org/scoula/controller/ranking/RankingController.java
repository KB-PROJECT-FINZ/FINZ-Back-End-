package org.scoula.controller.ranking;


import lombok.RequiredArgsConstructor;
import org.scoula.domain.Auth.vo.UserVo;
import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.RankingByTraitGroupDto;
import org.scoula.service.ranking.RankingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // 내 수익률(주간 기준)
    @GetMapping("/my")
    public ResponseEntity<MyRankingDto> getMyRanking(
            @RequestParam Long userId,
            @RequestParam(required = false) String baseDate) {

        MyRankingDto myRanking = rankingService.getMyRanking(userId, baseDate);
        return ResponseEntity.ok(myRanking);
    }

    // 인기 종목 (실시간 or fallback 지난주)
    @GetMapping("/popular-stocks")
    public List<PopularStockDto> getPopularStocks() {
        try {
            return rankingService.getRealTimeOrFallbackPopularStocks();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "인기 종목 조회 실패", e);
        }
    }

    // 주간 전체 랭킹
    @GetMapping("/weekly")
    public List<RankingByTraitGroupDto> getWeeklyRanking(@RequestParam(required = false) String baseDate) {
        return rankingService.getWeeklyRanking(baseDate);
    }

    // 주간 성향별 랭킹
    @GetMapping("/weekly/grouped")
    public Map<String, List<RankingByTraitGroupDto>> getGroupedRanking(@RequestParam(required = false) String baseDate) {
        return rankingService.getGroupedWeeklyRanking(baseDate);
    }
}