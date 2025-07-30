package org.scoula.util.mocktrading;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 가상 계좌번호 생성 유틸리티 클래스
 */
public class AccountNumberGenerator {

    private static final String BANK_CODE = "999";  // 가상 은행 코드
    private static final SecureRandom random = new SecureRandom();

    /**
     * 고유한 가상 계좌번호 생성
     * 형식: 999-YYMMDD-XXXXXX (총 20자)
     *
     * @return 생성된 계좌번호
     */
    public static String generateAccountNumber() {
        // 현재 날짜 (YYMMDD 형식)
        String dateStr = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMdd"));

        // 6자리 랜덤 숫자
        String randomStr = String.format("%06d", random.nextInt(1000000));

        return BANK_CODE + "-" + dateStr + "-" + randomStr;
    }

    /**
     * 계좌번호 중복 체크를 위한 재생성 메서드
     *
     * @param existingNumbers 기존 계좌번호 목록 (중복 체크용)
     * @return 중복되지 않는 계좌번호
     */
    public static String generateUniqueAccountNumber(java.util.Set<String> existingNumbers) {
        String accountNumber;
        int attempts = 0;
        final int MAX_ATTEMPTS = 100;

        do {
            accountNumber = generateAccountNumber();
            attempts++;

            if (attempts > MAX_ATTEMPTS) {
                throw new RuntimeException("계좌번호 생성 실패: 최대 시도 횟수 초과");
            }
        } while (existingNumbers != null && existingNumbers.contains(accountNumber));

        return accountNumber;
    }
}