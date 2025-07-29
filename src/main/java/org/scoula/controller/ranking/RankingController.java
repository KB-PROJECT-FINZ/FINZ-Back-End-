package org.scoula.controller.ranking;


import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.service.ranking.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    @Autowired
    private final RankingService rankingService;

    // 내 수익률 및 순위
    @GetMapping("/user/{userId}")
    public MyRankingDto getMyRanking(@PathVariable Long userId) {
        return rankingService.getMyRanking(userId);
    }

    // 전체 인기 종목 Top5 (매수 + 매도 합산 기준)
    @GetMapping("/top5")
    public List<PopularStockDto> getTop5Stocks(@RequestParam String dateType, @RequestParam String baseDate) {
        return rankingService.getTop5Stocks(dateType, baseDate);
    }

    // 전체 사용자 랭킹 Top100
    @GetMapping("/ranking")
    public List<RankingByTraitGroupDto> getAllRanking(@RequestParam String dateType, @RequestParam String baseDate) {
        return rankingService.getAllRanking(dateType, baseDate);
    }

    // 성향 그룹별 사용자 랭킹 Top100
    @GetMapping("/ranking/grouped")
    public Map<String, List<RankingByTraitGroupDto>> getGroupedRanking(@RequestParam String dateType, @RequestParam String baseDate) {
        return rankingService.getGroupedRanking(dateType, baseDate);
    }
}

