package org.scoula.service.mocktrading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.scoula.domain.mocktrading.vo.UserAccount;
import org.scoula.mapper.UserAccountMapper;
import org.scoula.util.mocktrading.AccountNumberGenerator;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserAccountService {

    private final UserAccountMapper userAccountMapper;

    // 초기 시드머니 (1천만원)
    private static final Long INITIAL_BALANCE = 10_000_000L;

    /**
     * 회원가입 완료 후 계좌 생성 (트랜잭션 독립적으로 처리)
     *
     * @param userId 사용자 ID
     * @return 생성된 계좌 정보
     */
    @Transactional
    public UserAccount createAccountForNewUser(Integer userId) {
        log.info("신규 회원 계좌 생성 시작 - 사용자 ID: {}", userId);

        try {
            // 기존 계좌 존재 여부 확인
            UserAccount existingAccount = userAccountMapper.selectByUserId(userId);
            if (existingAccount != null) {
                log.warn("이미 계좌가 존재합니다 - 사용자 ID: {}, 계좌번호: {}",
                        userId, existingAccount.getAccountNumber());
                return existingAccount;
            }

            // 고유한 계좌번호 생성
            String accountNumber = generateUniqueAccountNumber();

            // 새 계좌 생성
            UserAccount newAccount = UserAccount.builder()
                    .userId(userId)
                    .accountNumber(accountNumber)
                    .currentBalance(INITIAL_BALANCE)
                    .totalAssetValue(INITIAL_BALANCE)
                    .totalProfitLoss(0L)
                    .profitRate(BigDecimal.ZERO)
                    .resetCount(0)
                    .build();

            // 데이터베이스에 저장
            int result = userAccountMapper.insertUserAccount(newAccount);

            if (result > 0) {
                log.info("신규 회원 계좌 생성 완료 - 사용자 ID: {}, 계좌번호: {}",
                        userId, accountNumber);
                return newAccount;
            } else {
                log.error("계좌 생성 실패 - 사용자 ID: {}", userId);
                return null;
            }

        } catch (Exception e) {
            log.error("신규 회원 계좌 생성 중 오류 발생 - 사용자 ID: {}", userId, e);
            // 계좌 생성 실패해도 회원가입은 성공으로 처리
            return null;
        }
    }

    /**
     * 사용자 ID로 계좌 조회
     *
     * @param userId 사용자 ID
     * @return 계좌 정보
     */
    public UserAccount getUserAccount(Integer userId) {
        log.debug("사용자 계좌 조회 - 사용자 ID: {}", userId);
        return userAccountMapper.selectByUserId(userId);
    }

    /**
     * 계좌번호로 계좌 조회
     *
     * @param accountNumber 계좌번호
     * @return 계좌 정보
     */
    public UserAccount getUserAccountByNumber(String accountNumber) {
        log.debug("계좌 조회 - 계좌번호: {}", accountNumber);
        return userAccountMapper.selectByAccountNumber(accountNumber);
    }

    /**
     * 중복되지 않는 계좌번호 생성
     *
     * @return 고유한 계좌번호
     */
    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        final int MAX_ATTEMPTS = 100;

        do {
            accountNumber = AccountNumberGenerator.generateAccountNumber();
            attempts++;

            if (attempts > MAX_ATTEMPTS) {
                throw new RuntimeException("계좌번호 생성 실패: 최대 시도 횟수 초과");
            }
        } while (userAccountMapper.selectByAccountNumber(accountNumber) != null);

        return accountNumber;
    }

    /**
     * 계좌 잔액 업데이트
     *
     * @param userId 사용자 ID
     * @param newBalance 새로운 잔액
     * @return 업데이트 성공 여부
     */
    @Transactional
    public boolean updateBalance(Integer userId, Long newBalance) {
        log.info("계좌 잔액 업데이트 - 사용자 ID: {}, 새 잔액: {}", userId, newBalance);

        try {
            int result = userAccountMapper.updateBalance(userId, newBalance);
            return result > 0;
        } catch (Exception e) {
            log.error("계좌 잔액 업데이트 중 오류 발생 - 사용자 ID: {}", userId, e);
            return false;
        }
    }
}