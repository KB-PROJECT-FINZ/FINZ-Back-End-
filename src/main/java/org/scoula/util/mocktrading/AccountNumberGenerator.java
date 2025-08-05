package org.scoula.util.mocktrading;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AccountNumberGenerator {

    private static final SecureRandom random = new SecureRandom();

    /**
     * 고유한 가상 계좌번호 생성
     * 형식: YYYYMMDD-XXXXXX (총 15자리)
     * 예시: 20241130-123456
     */
    public static String generate() {
        // 현재 날짜 (YYYYMMDD)
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 6자리 랜덤 숫자
        String randomStr = String.format("%06d", random.nextInt(1000000));

        return String.format("%s-%s", dateStr, randomStr);
    }

    /**
     * 마스킹된 계좌번호 반환 (보안을 위해 일부 숫자 숨김)
     * 예시: 20241130-123456 → ****1130-***456
     */
    public static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 15) {
            return accountNumber;
        }

        try {
            String[] parts = accountNumber.split("-");
            if (parts.length != 2) {
                return accountNumber;
            }

            String datePart = parts[0];
            String randomPart = parts[1];

            // 날짜 부분 마스킹 (앞 4자리 숨김)
            String maskedDate = "****" + datePart.substring(4);

            // 랜덤 부분 마스킹 (앞 3자리 숨김)
            String maskedRandom = "***" + randomPart.substring(3);

            return String.format("%s-%s", maskedDate, maskedRandom);

        } catch (Exception e) {
            return accountNumber; // 에러 시 원본 반환
        }
    }

    /**
     * 계좌번호 유효성 검증
     */
    public static boolean isValid(String accountNumber) {
        if (accountNumber == null) {
            return false;
        }

        // 기본 형식 확인: YYYYMMDD-XXXXXX
        String pattern = "^\\d{8}-\\d{6}$";
        return accountNumber.matches(pattern);
    }

    /**
     * 표시용 계좌번호 포맷 (4자리씩 구분)
     * 예시: 20241130-123456 → 2024-1130-1234-56
     */
    public static String formatForDisplay(String accountNumber) {
        if (!isValid(accountNumber)) {
            return accountNumber;
        }

        try {
            String[] parts = accountNumber.split("-");
            String datePart = parts[0];
            String randomPart = parts[1];

            // 날짜를 4자리씩 분할
            String year = datePart.substring(0, 4);
            String monthDay = datePart.substring(4, 8);

            // 랜덤 부분을 4자리, 2자리로 분할
            String randomFirst = randomPart.substring(0, 4);
            String randomSecond = randomPart.substring(4, 6);

            return String.format("%s-%s-%s-%s", year, monthDay, randomFirst, randomSecond);

        } catch (Exception e) {
            return accountNumber;
        }
    }
}