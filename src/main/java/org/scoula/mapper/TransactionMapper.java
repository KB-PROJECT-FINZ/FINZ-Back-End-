package org.scoula.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.domain.mocktrading.vo.Transaction;

import java.util.List;

public interface TransactionMapper {

    /**
     * 사용자 ID로 거래 내역 조회 (페이징)
     */
    List<Transaction> selectByUserId(@Param("userId") Integer userId,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    /**
     * 계좌 ID로 거래 내역 조회 (페이징)
     */
    List<Transaction> selectByAccountId(@Param("accountId") Integer accountId,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    /**
     * 사용자 ID와 종목코드로 거래 내역 조회
     */
    List<Transaction> selectByUserAndStock(@Param("userId") Integer userId,
                                           @Param("stockCode") String stockCode,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    /**
     * 사용자 ID와 기간으로 거래 내역 조회 (최근 N일)
     */
    List<Transaction> selectByUserIdAndPeriod(@Param("userId") Integer userId,
                                              @Param("days") int days);

    /**
     * 거래 내역 ID로 조회
     */
    Transaction selectByTransactionId(@Param("transactionId") Integer transactionId);

    /**
     * 거래 내역 추가
     */
    int insertTransaction(Transaction transaction);

    /**
     * 거래 내역 업데이트
     */
    int updateTransaction(Transaction transaction);

    /**
     * 거래 내역 삭제
     */
    int deleteTransaction(@Param("transactionId") Integer transactionId);

    /**
     * 계좌의 모든 거래 내역 삭제 (계좌 초기화 시)
     */
    int deleteAllByAccount(@Param("accountId") Integer accountId);

    /**
     * 사용자의 총 거래 횟수
     */
    int countByUserId(@Param("userId") Integer userId);

    /**
     * 거래 타입별 총 금액 계산
     */
    Long sumByUserIdAndType(@Param("userId") Integer userId,
                            @Param("transactionType") String transactionType,
                            @Param("days") int days);

    /**
     * 거래 타입별 거래 횟수
     */
    int countByUserIdAndType(@Param("userId") Integer userId,
                             @Param("transactionType") String transactionType,
                             @Param("days") int days);
}