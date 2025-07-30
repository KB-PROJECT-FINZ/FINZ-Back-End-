package org.scoula.domain.mocktrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {
    private Integer accountId;         // 계좌 ID
    private Integer userId;            // 사용자 ID
    private String accountNumber;      // 가상 계좌번호
    private Long currentBalance;       // 현재 보유 현금
    private Long totalAssetValue;      // 총 자산 가치
    private Long totalProfitLoss;      // 총 손익
    private BigDecimal profitRate;     // 수익률 (%)
    private Integer resetCount;        // 초기화 횟수
    private Timestamp createdAt;       // 생성 날짜
    private Timestamp updatedAt;       // 수정 날짜
}