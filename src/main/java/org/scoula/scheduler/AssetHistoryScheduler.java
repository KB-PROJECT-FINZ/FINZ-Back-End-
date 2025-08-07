package org.scoula.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.service.ranking.AssetHistoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


// 랭킹을 위한 주간 데이터 저장용
@Log4j2
@Component
@RequiredArgsConstructor
public class AssetHistoryScheduler {

    private final AssetHistoryService assetHistoryService;

    //매주 월요일 0시 5분에 실행
    @Scheduled(cron = "0 5 0 * * MON")
    public void runWeeklyAssetHistorySave() {
        log.info(">>> WeeklyAssetScheduler 실행: 주간 자산 수익률 저장 시작");
        try {
            assetHistoryService.saveWeeklyAssetHistory();
            log.info(">>> WeeklyAssetScheduler 완료: 주간 자산 수익률 저장 성공");
        } catch (Exception e) {
            log.error(">>> WeeklyAssetScheduler 오류 발생", e);
        }
    }
}
