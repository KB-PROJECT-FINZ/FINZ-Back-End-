package org.scoula.service.mocktrading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.scoula.domain.mocktrading.vo.UserAccount;
import org.scoula.domain.mocktrading.vo.Holding;
import org.scoula.mapper.UserAccountMapper;
import org.scoula.util.mocktrading.AccountNumberGenerator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserAccountService {

    private final UserAccountMapper userAccountMapper;

    @Autowired
    @Lazy
    private HoldingService holdingService;

    @Autowired
    @Lazy
    private TransactionService transactionService;

    // 초기 시드머니 (1천만원)
    private static final Long INITIAL_BALANCE = 10_000_000L;

    /**
     * 회원가입 완료 후 계좌 생성 (트랜잭션 독립적으로 처리)
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
            return null;
        }
    }
    /**
     * 사용자 크레딧 조회
     */
    public Integer getUserCredit(Integer userId) {
        try {
            log.debug("사용자 크레딧 조회 - 사용자 ID: {}", userId);
            Integer credit = userAccountMapper.getUserCredit(userId);
            return credit != null ? credit : 0;
        } catch (Exception e) {
            log.error("사용자 크레딧 조회 실패 - 사용자 ID: {}", userId, e);
            return 0;
        }
    }
    /**
     * 사용자 ID로 계좌 조회
     */
    public UserAccount getUserAccount(Integer userId) {
        log.debug("사용자 계좌 조회 - 사용자 ID: {}", userId);
        return userAccountMapper.selectByUserId(userId);
    }

    /**
     * 계좌 ID로 계좌 정보 조회
     */
    public UserAccount getAccountById(Integer accountId) {
        try {
            log.debug("계좌 ID로 계좌 조회 - 계좌 ID: {}", accountId);
            return userAccountMapper.selectByAccountId(accountId);
        } catch (Exception e) {
            log.error("계좌 조회 실패 - 계좌 ID: {}", accountId, e);
            return null;
        }
    }

    /**
     * 크레딧 충전 (포인트를 현금으로 전환)
     */
    @Transactional
    public boolean chargeCredit(Integer userId, Integer creditAmount) {
        try {
            log.info("크레딧 충전 시작 - 사용자 ID: {}, 충전 크레딧: {}", userId, creditAmount);

            // 1. 사용자 크레딧 확인 및 차감
            boolean creditDeducted = deductUserCredit(userId, creditAmount);
            if (!creditDeducted) {
                log.warn("크레딧 부족 또는 차감 실패 - 사용자 ID: {}", userId);
                return false;
            }

            // 2. 계좌 잔고에 현금 추가 (1크레딧 = 1,000원)
            long cashAmount = (long) creditAmount * 1000;
            UserAccount account = getUserAccount(userId);

            if (account == null) {
                account = createAccountForNewUser(userId);
                if (account == null) {
                    log.error("계좌 생성 실패 - 크레딧 충전 중단");
                    return false;
                }
            }

            // 3. 계좌 잔고 업데이트
            boolean balanceUpdated = updateBalance(account.getAccountId(), cashAmount);
            if (!balanceUpdated) {
                log.error("계좌 잔고 업데이트 실패");
                return false;
            }

            log.info("크레딧 충전 완료 - 사용자 ID: {}, 충전 크레딧: {}, 현금: {}원",
                    userId, creditAmount, cashAmount);

            return true;

        } catch (Exception e) {
            log.error("크레딧 충전 실패 - 사용자 ID: {}, 크레딧: {}", userId, creditAmount, e);
            throw new RuntimeException("크레딧 충전 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 크레딧 차감
     */
    private boolean deductUserCredit(Integer userId, Integer creditAmount) {
        try {
            int result = userAccountMapper.deductUserCredit(userId, creditAmount);
            return result > 0;
        } catch (Exception e) {
            log.error("사용자 크레딧 차감 실패 - 사용자 ID: {}, 크레딧: {}", userId, creditAmount, e);
            return false;
        }
    }

    /**
     * 계좌 잔고 업데이트
     */
    @Transactional
    public boolean updateBalance(Integer accountId, Long amount) {
        try {
            log.debug("계좌 잔고 업데이트 - 계좌 ID: {}, 금액: {}", accountId, amount);

            int result = userAccountMapper.updateBalance(accountId, amount);

            if (result > 0) {
                updateTotalAssetValue(accountId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("계좌 잔고 업데이트 실패 - 계좌 ID: {}, 금액: {}", accountId, amount, e);
            return false;
        }
    }

    /**
     * 총 자산가치 업데이트
     */
    @Transactional
    public void updateTotalAssetValue(Integer accountId) {
        try {
            log.debug("총 자산가치 업데이트 - 계좌 ID: {}", accountId);

            UserAccount account = userAccountMapper.selectByAccountId(accountId);
            if (account == null) {
                log.warn("계좌 정보를 찾을 수 없습니다 - 계좌 ID: {}", accountId);
                return;
            }

            // 1. 주식 현재 평가금액 계산
            Long stockValue = userAccountMapper.calculateStockValue(accountId);
            if (stockValue == null) {
                stockValue = 0L;
            }

            // 2. 총 자산 = 현금 잔고 + 주식 평가금액
            Long totalAssetValue = account.getCurrentBalance() + stockValue;

            // 3. ✅ 실제 투자한 원금만 계산 (holdings의 total_cost 합계)
            Long totalInvestedAmount = userAccountMapper.calculateTotalInvestedAmount(accountId);
            if (totalInvestedAmount == null) {
                totalInvestedAmount = 0L;
            }

            // 4. ✅ 올바른 손익 및 수익률 계산
            Long totalProfitLoss;
            BigDecimal profitRate = BigDecimal.ZERO;

            if (totalInvestedAmount > 0) {
                // 투자한 주식이 있는 경우만 손익 계산
                totalProfitLoss = stockValue - totalInvestedAmount;

                // 수익률 = (주식 평가손익 / 실제 투자금액) * 100
                profitRate = BigDecimal.valueOf(totalProfitLoss)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalInvestedAmount), 2, BigDecimal.ROUND_HALF_UP);
            } else {
                // 투자하지 않은 경우: 손익 0, 수익률 0
                totalProfitLoss = 0L;
                profitRate = BigDecimal.ZERO;
            }

            userAccountMapper.updateAssetInfo(accountId, totalAssetValue, totalProfitLoss, profitRate);

            log.info("자산가치 업데이트 완료 - 계좌 ID: {}", accountId);
            log.info("- 총자산: {} (현금: {} + 주식: {})", totalAssetValue, account.getCurrentBalance(), stockValue);
            log.info("- 실제투자금: {}, 투자손익: {}, 수익률: {}%", totalInvestedAmount, totalProfitLoss, profitRate);

        } catch (Exception e) {
            log.error("총 자산가치 업데이트 실패 - 계좌 ID: {}", accountId, e);
        }
    }

    /**
     * 고유한 계좌번호 생성
     */
    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        final int MAX_ATTEMPTS = 10;

        do {
            accountNumber = AccountNumberGenerator.generate();
            attempts++;

            if (attempts > MAX_ATTEMPTS) {
                throw new RuntimeException("계좌번호 생성에 실패했습니다.");
            }

        } while (userAccountMapper.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}