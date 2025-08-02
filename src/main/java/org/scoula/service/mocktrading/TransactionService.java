package org.scoula.service.mocktrading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.mocktrading.vo.Transaction;
import org.scoula.domain.mocktrading.vo.UserAccount;
import org.scoula.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionService {

    private final TransactionMapper transactionMapper;

    // 순환 참조 방지를 위한 지연 로딩
    @Autowired
    @Lazy
    private UserAccountService userAccountService;

    @Autowired
    @Lazy
    private HoldingService holdingService;

    /**
     * 사용자의 거래 내역 조회
     */
    public List<Transaction> getUserTransactions(Integer userId, int limit, int offset) {
        try {
            log.debug("사용자 거래 내역 조회 - 사용자 ID: {}, limit: {}, offset: {}", userId, limit, offset);

            return transactionMapper.selectByUserId(userId, limit, offset);

        } catch (Exception e) {
            log.error("거래 내역 조회 실패 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("거래 내역 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 종목의 거래 내역 조회
     */
    public List<Transaction> getUserTransactionsByStock(Integer userId, String stockCode, int limit, int offset) {
        try {
            log.debug("종목별 거래 내역 조회 - 사용자 ID: {}, 종목: {}", userId, stockCode);

            return transactionMapper.selectByUserAndStock(userId, stockCode, limit, offset);

        } catch (Exception e) {
            log.error("종목별 거래 내역 조회 실패 - 사용자 ID: {}, 종목: {}", userId, stockCode, e);
            throw new RuntimeException("거래 내역 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 주문 처리 (매수/매도)
     */
    @Transactional
    public boolean processOrder(Integer userId, String stockCode, String stockName,
                                String transactionType, String orderType,
                                Integer quantity, Integer price, Integer orderPrice) {
        try {
            log.info("주문 처리 시작 - 사용자 ID: {}, 종목: {}, 타입: {}, 수량: {}, 가격: {}",
                    userId, stockCode, transactionType, quantity, price);

            // 사용자 계좌 정보 조회
            UserAccount account = userAccountService.getUserAccount(userId);
            if (account == null) {
                log.error("계좌 정보를 찾을 수 없습니다 - 사용자 ID: {}", userId);
                return false;
            }

            // 거래 타입별 처리
            boolean orderSuccess = false;
            if ("BUY".equals(transactionType)) {
                orderSuccess = processBuyOrder(account, stockCode, stockName, orderType, quantity, price, orderPrice);
            } else if ("SELL".equals(transactionType)) {
                orderSuccess = processSellOrder(account, stockCode, stockName, orderType, quantity, price, orderPrice);
            } else {
                log.error("유효하지 않은 거래 타입: {}", transactionType);
                return false;
            }

            // 거래 성공 시 거래 내역 저장
            if (orderSuccess) {
                long totalAmount = (long) quantity * price;

                Transaction transaction = Transaction.builder()
                        .accountId(account.getAccountId())
                        .stockCode(stockCode)
                        .stockName(stockName)
                        .transactionType(transactionType)
                        .orderType(orderType)
                        .quantity(quantity)
                        .price(price)
                        .orderPrice(orderPrice)
                        .totalAmount(totalAmount)
                        .build();

                int result = transactionMapper.insertTransaction(transaction);
                if (result <= 0) {
                    log.error("거래 내역 저장 실패");
                    throw new RuntimeException("거래 내역 저장에 실패했습니다.");
                }

                log.info("주문 처리 완료 - 거래 ID: {}", transaction.getTransactionId());
            }

            return orderSuccess;

        } catch (Exception e) {
            log.error("주문 처리 실패", e);
            throw new RuntimeException("주문 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 매수 주문 처리
     */
    private boolean processBuyOrder(UserAccount account, String stockCode, String stockName,
                                    String orderType, Integer quantity, Integer price, Integer orderPrice) {

        long totalCost = (long) quantity * price;

        // 잔고 확인
        if (account.getCurrentBalance() < totalCost) {
            log.warn("잔고 부족 - 현재 잔고: {}, 필요 금액: {}", account.getCurrentBalance(), totalCost);
            return false;
        }

        // 계좌 잔고 차감
        boolean balanceUpdated = userAccountService.updateBalance(
                account.getAccountId(), -totalCost);

        if (!balanceUpdated) {
            log.error("계좌 잔고 업데이트 실패");
            return false;
        }

        // 보유 종목 추가/업데이트
        holdingService.addOrUpdateHolding(account.getAccountId(), stockCode, stockName, quantity, price);

        log.info("매수 주문 완료 - 종목: {}, 수량: {}, 가격: {}, 총액: {}",
                stockCode, quantity, price, totalCost);

        return true;
    }

    /**
     * 매도 주문 처리
     */
    private boolean processSellOrder(UserAccount account, String stockCode, String stockName,
                                     String orderType, Integer quantity, Integer price, Integer orderPrice) {

        // 보유 종목 확인 및 수량 차감
        boolean holdingReduced = holdingService.reduceHolding(
                account.getAccountId(), stockCode, quantity, price);

        if (!holdingReduced) {
            log.warn("보유 종목 부족 또는 매도 실패 - 종목: {}, 수량: {}", stockCode, quantity);
            return false;
        }

        // 매도 금액 계좌에 추가
        long totalAmount = (long) quantity * price;
        boolean balanceUpdated = userAccountService.updateBalance(
                account.getAccountId(), totalAmount);

        if (!balanceUpdated) {
            log.error("계좌 잔고 업데이트 실패");
            return false;
        }

        log.info("매도 주문 완료 - 종목: {}, 수량: {}, 가격: {}, 총액: {}",
                stockCode, quantity, price, totalAmount);

        return true;
    }

    /**
     * 최근 거래 내역 요약 (대시보드용)
     */
    public TransactionSummary getRecentTransactionSummary(Integer userId, int days) {
        try {
            List<Transaction> recentTransactions = transactionMapper.selectByUserIdAndPeriod(userId, days);

            long totalBuyAmount = 0;
            long totalSellAmount = 0;
            int buyCount = 0;
            int sellCount = 0;

            for (Transaction transaction : recentTransactions) {
                if ("BUY".equals(transaction.getTransactionType())) {
                    totalBuyAmount += transaction.getTotalAmount();
                    buyCount++;
                } else if ("SELL".equals(transaction.getTransactionType())) {
                    totalSellAmount += transaction.getTotalAmount();
                    sellCount++;
                }
            }

            return TransactionSummary.builder()
                    .period(days)
                    .totalTransactions(recentTransactions.size())
                    .buyCount(buyCount)
                    .sellCount(sellCount)
                    .totalBuyAmount(totalBuyAmount)
                    .totalSellAmount(totalSellAmount)
                    .netAmount(totalSellAmount - totalBuyAmount)
                    .build();

        } catch (Exception e) {
            log.error("최근 거래 요약 조회 실패 - 사용자 ID: {}, 기간: {}", userId, days, e);
            return TransactionSummary.builder()
                    .period(days)
                    .totalTransactions(0)
                    .buyCount(0)
                    .sellCount(0)
                    .totalBuyAmount(0L)
                    .totalSellAmount(0L)
                    .netAmount(0L)
                    .build();
        }
    }
    /**
     * 거래 요약 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransactionSummary {
        private int period;            // 조회 기간 (일수)
        private int totalTransactions; // 총 거래 횟수
        private int buyCount;          // 매수 횟수
        private int sellCount;         // 매도 횟수
        private long totalBuyAmount;   // 총 매수 금액
        private long totalSellAmount;  // 총 매도 금액
        private long netAmount;        // 순 거래 금액
    }
}