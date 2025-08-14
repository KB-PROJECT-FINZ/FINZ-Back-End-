package org.scoula.mapper.trading;

import org.apache.ibatis.annotations.Param;
import org.scoula.domain.mocktrading.vo.Holding;

import java.util.List;

public interface HoldingMapper {

    /**
     * 사용자 ID로 보유 종목 조회
     */
    List<Holding> selectByUserId(@Param("userId") Integer userId);

    /**
     * 계좌 ID로 보유 종목 조회
     */
    List<Holding> selectByAccountId(@Param("accountId") Integer accountId);

    /**
     * 사용자 ID와 종목코드로 보유 종목 조회
     */
    Holding selectByUserAndStock(@Param("userId") Integer userId, @Param("stockCode") String stockCode);

    /**
     * 계좌 ID와 종목코드로 보유 종목 조회
     */
    Holding selectByAccountAndStock(@Param("accountId") Integer accountId, @Param("stockCode") String stockCode);

    /**
     * 보유 종목 ID로 조회
     */
    Holding selectByHoldingId(@Param("holdingId") Integer holdingId);

    /**
     * 보유 종목 추가
     */
    int insertHolding(Holding holding);

    /**
     * 보유 종목 업데이트
     */
    int updateHolding(Holding holding);

    /**
     * 보유 종목 삭제
     */
    int deleteHolding(@Param("holdingId") Integer holdingId);

    /**
     * 계좌의 모든 보유 종목 삭제 (계좌 초기화 시)
     */
    int deleteAllByAccount(@Param("accountId") Integer accountId);

    /**
     * 사용자의 총 보유 종목 수
     */
    int countByUserId(@Param("userId") Integer userId);

    /**
     * 사용자의 총 주식 평가금액 계산
     */
    Long calculateTotalStockValue(@Param("userId") Integer userId);
}