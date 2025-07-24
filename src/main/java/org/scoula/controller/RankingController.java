package org.scoula.controller;


import org.scoula.domain.MyRankingDto;
import org.scoula.domain.Top5StockDto;
import org.scoula.domain.WeeklyRankingDto;
import org.scoula.service.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ranking")
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
    public List<Top5StockDto>getTop5Stock(@RequestParam String week, @RequestParam String traitType){
        return rankingService.getTop5Stocks(week,traitType);
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
