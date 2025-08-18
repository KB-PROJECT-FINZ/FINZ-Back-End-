package org.scoula.mapper.ranking;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.ranking.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {
    /** 성향별 보유 비중 조회 */
    List<TraitStockDto> findTraitStocks(@Param("userId") Long userId);

    /** 내 수익률 분포(최신 스냅샷 + 집계 조인) */
    List<MyDistributionDto> findMyDistribution(@Param("userId") Long userId);

    /** 유사 성향 투자자 인기 종목 조회 */
    List<PopularStockDto> findPopularStocksByTrait(@Param("traitGroup") String traitGroup);

    /** 전체 보유 종목 평균 매입가 조회 */
    List<HoldingSummaryDto> findHoldingSummaries();

    /** 종목별 수익률 upsert */
    void upsertStockProfitRate(@Param("stockCode") String stockCode,
                               @Param("profitRate") double profitRate);

    /** 전체 사용자 ID 리스트 */
    List<Integer> findAllUserIds();

    /** 종목 분포 집계 (유저별 최신 1건 기준) */
    List<StockDistributionSummaryDto> aggregateAllStockDistributions();

    /** 사용자 보유 종목 */
    List<HoldingSummaryDto> findHoldingsByUserId(@Param("userId") int userId);

    /** 사용자별 수익률 분포 upsert (개인 스냅샷) */
    void upsertMyStockDistribution(@Param("userId") Long userId,
                                   @Param("distribution") MyDistributionDto distribution);
}