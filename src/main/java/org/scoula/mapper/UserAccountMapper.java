package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.scoula.domain.mocktrading.vo.UserAccount;

@Mapper
public interface UserAccountMapper {

    /**
     * 새로운 가상 계좌 생성
     * @param userAccount 계좌 정보
     * @return 생성된 행의 수
     */
    int insertUserAccount(UserAccount userAccount);

    /**
     * 사용자 ID로 계좌 조회
     * @param userId 사용자 ID
     * @return 계좌 정보
     */
    UserAccount selectByUserId(Integer userId);

    /**
     * 계좌번호로 계좌 조회
     * @param accountNumber 계좌번호
     * @return 계좌 정보
     */
    UserAccount selectByAccountNumber(String accountNumber);

    /**
     * 계좌 정보 업데이트
     * @param userAccount 업데이트할 계좌 정보
     * @return 업데이트된 행의 수
     */
    int updateUserAccount(UserAccount userAccount);

    /**
     * 계좌 잔액 업데이트
     * @param userId 사용자 ID
     * @param currentBalance 현재 잔액
     * @return 업데이트된 행의 수
     */
    int updateBalance(Integer userId, Long currentBalance);
}