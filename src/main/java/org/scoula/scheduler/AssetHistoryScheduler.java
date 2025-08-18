package org.scoula.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.service.ranking.AssetHistoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;


// 랭킹을 위한 주간 데이터 저장용
@Log4j2
@Component
@RequiredArgsConstructor
public class AssetHistoryScheduler {

    private final AssetHistoryService assetHistoryService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 매주 월요일 00:05 (KST) 실행
     */
    @Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Seoul")
    public void runWeeklyAssetHistorySave() {
        log.info(">>> WeeklyAssetScheduler 실행: 주간 자산 수익률 저장 시작");
        try {
            LocalDate today = LocalDate.now(KST);
            int dow0 = today.getDayOfWeek().getValue() % 7; // SUN=0
            // ✅ 지난주 일요일(완결 주) = 오늘 주의 일요일에서 1주 전
            LocalDate anchorSunday = today.minusDays(dow0 + 7);
            log.info(">>> weekly anchorSunday={}", anchorSunday);

            assetHistoryService.saveWeeklyAssetHistory(anchorSunday);

            log.info(">>> WeeklyAssetScheduler 완료: 주간 자산 수익률 저장 성공 (anchor={})", anchorSunday);
        } catch (Exception e) {
            log.error(">>> WeeklyAssetScheduler 오류 발생", e);
        }
    }
}