package org.scoula.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.domain.mocktrading.vo.UserAccount;

import java.math.BigDecimal;
import java.util.List;

public interface UserAccountMapper {

    /**
     * 가상 계좌 생성
     */
    int insertUserAccount(UserAccount userAccount);

    /**
     * 사용자 ID로 계좌 조회
     */
    UserAccount selectByUserId(@Param("userId") Integer userId);

    /**
     * 계좌 ID로 계좌 조회
     */
    UserAccount selectByAccountId(@Param("accountId") Integer accountId);

    /**
     * 계좌번호로 계좌 조회
     */
    UserAccount selectByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * 계좌번호 중복 확인
     */
    boolean existsByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * 계좌 정보 업데이트
     */
    int updateUserAccount(UserAccount userAccount);

    /**
     * 사용자 크레딧 차감
     */
    int deductUserCredit(@Param("userId") Integer userId, @Param("creditAmount") Integer creditAmount);

    /**
     * 계좌 잔고 업데이트 (금액 증감)
     */
    int updateBalance(@Param("accountId") Integer accountId, @Param("amount") Long amount);

    /**
     * 계좌 잔액만 업데이트 (기존 메서드 - 호환성 유지)
     */
    int updateBalanceOnly(@Param("userId") Integer userId, @Param("currentBalance") Long currentBalance);

    /**
     * 계좌의 주식 평가금액 계산
     */
    Long calculateStockValue(@Param("accountId") Integer accountId);

    /**
     * 자산 정보 업데이트 (총자산, 손익, 수익률)
     */
    int updateAssetInfo(@Param("accountId") Integer accountId,
                        @Param("totalAssetValue") Long totalAssetValue,
                        @Param("totalProfitLoss") Long totalProfitLoss,
                        @Param("profitRate") BigDecimal profitRate);

    /**
     * 계좌 초기화
     */
    int resetAccount(UserAccount account);

    /**
     * 사용자별 계좌 수 조회
     */
    int countByUserId(@Param("userId") Integer userId);

    /**
     * 전체 사용자 수 조회 (통계용)
     */
    int getTotalUserCount();

    /**
     * 총 자산 순위 조회 (TOP N)
     */
    List<UserAccount> getAssetRanking(@Param("limit") int limit);

    /**
     * 수익률 순위 조회 (TOP N)
     */
    List<UserAccount> getProfitRateRanking(@Param("limit") int limit);

    /**
     * 특정 사용자의 자산 순위 조회
     */
    int getUserAssetRank(@Param("userId") Integer userId);

    /**
     * 특정 사용자의 수익률 순위 조회
     */
    int getUserProfitRateRank(@Param("userId") Integer userId);

    Integer getUserCredit(Integer userId);

    Long calculateTotalInvestedAmount(@Param("accountId") Integer accountId);
}