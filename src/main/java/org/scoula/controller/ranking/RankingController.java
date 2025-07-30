package org.scoula.controller.ranking;


import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.RankingByTraitGroupDto;
import org.scoula.service.ranking.RankingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/my")
    public MyRankingDto getMyRanking(@RequestParam Long userId,
                                     @RequestParam(required = false) String recordDate) {
        return rankingService.getMyRanking(userId, recordDate);
    }
    @GetMapping("/popular-stocks")
    public List<PopularStockDto> getTop5Stocks() {
        return rankingService.getTop5Stocks();
    }

    @GetMapping("/weekly")
    public List<RankingByTraitGroupDto> getWeeklyRanking() {
        return rankingService.getWeeklyRanking();
    }
    @GetMapping("/weekly/grouped")
    public Map<String, List<RankingByTraitGroupDto>> getGroupedRanking() {
        return rankingService.getGroupedWeeklyRanking();
    }
}