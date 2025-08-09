package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {
    /**
     * 성향별 보유 비중 조회
     */
    List<TraitStockDto> findTraitStocks(@Param("userId") Long userId);

    /**
     * 내 수익률 분포 조회
     */
    List<MyDistributionDto> findMyDistribution(@Param("userId") Long userId);

    /**
     * 유사 성향 투자자 인기 종목 조회
     */
    List<PopularStockDto> findPopularStocksByTrait(@Param("traitGroup") String traitGroup);

    // (1) 전체 보유 종목 평균 매입가 조회
    List<HoldingSummaryDto> findHoldingSummaries();

    // (2) 종목별 수익률 저장 또는 업데이트 (upsert)
    void upsertStockProfitRate(@Param("stockCode") String stockCode,
                               @Param("profitRate") double profitRate);

    // (3) 전체 사용자 ID 리스트 조회
    List<Integer> findAllUserIds();

    // 전체 종목 분포 집계
    List<StockDistributionSummaryDto> aggregateAllStockDistributions();

    // 사용자별 종목 분포 집계
    List<StockDistributionSummaryDto> aggregateUserStockDistributions(@Param("userId") Long userId);

    // (5) 특정 사용자 보유 종목 조회
    List<HoldingSummaryDto> findHoldingsByUserId(@Param("userId") int userId);

    // (6) 사용자별 수익률 분포 저장 또는 업데이트 (upsert)
    void upsertMyStockDistribution(@Param("userId") Long userId,
                                   @Param("distribution") MyDistributionDto distribution);
}

