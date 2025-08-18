package org.scoula.service.ranking;

import java.time.LocalDate;

public interface AssetHistoryService {
    /** 주간 데이터 저장 (기본: 지난주 일요일, KST 기준) */
    void saveWeeklyAssetHistory();

    /** 주간 데이터 저장 (앵커 일요일을 명시적으로 지정) */
    void saveWeeklyAssetHistory(LocalDate anchorSunday);

    /** (기존) 이번 주 자산 스냅샷 저장 — 필요 시 유지 */
    void saveAssetHistoryForAllUsersThisWeek();
}
