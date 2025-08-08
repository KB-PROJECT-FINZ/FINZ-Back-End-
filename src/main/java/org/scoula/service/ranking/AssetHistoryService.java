package org.scoula.service.ranking;

import java.time.LocalDate;

public interface AssetHistoryService {
    //주간 데이터 저장
    void saveWeeklyAssetHistory();

    void saveAssetHistoryForAllUsersThisWeek();
}
