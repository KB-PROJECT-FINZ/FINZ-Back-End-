package org.scoula.controller.ranking;


import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.MyRankingDto;
import org.scoula.domain.ranking.Top5StockDto;
import org.scoula.domain.ranking.WeeklyRankingDto;
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
    private RankingService rankingService;

    // 내수익률 및 순위

    @GetMapping("/user/{userId}")
    public MyRankingDto getMyRanking(@PathVariable Long userId) {
        return rankingService.getMyRanking(userId);
    }

    //인기 종목 Top5

    @GetMapping("/top5")
    public List<Top5StockDto>getTop5Stock(@RequestParam String week){
        return rankingService.getTop5Stocks(week);
    }

    //주간 랭킹
    @GetMapping("/weekly")
    public List<WeeklyRankingDto> getWeeklyRanking(@RequestParam String week){

        return rankingService.getWeeklyRanking(week);
    }

    //주간 성향별 랭킹
    @GetMapping("/weekly/grouped")
    public Map<String, List<WeeklyRankingDto>> getGroupedWeeklyRanking(@RequestParam String week) {
        return rankingService.getGroupedWeeklyRanking(week);
    }


}
