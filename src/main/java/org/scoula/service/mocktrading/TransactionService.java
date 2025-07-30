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
     * 계좌별 거래 내역 조회
     */
    public List<Transaction> getAccountTransactions(Integer accountId, int limit, int offset) {
        try {
            log.debug("계좌별 거래 내역 조회 - 계좌 ID: {}, limit: {}, offset: {}", accountId, limit, offset);

            return transactionMapper.selectByAccountId(accountId, limit, offset);

        } catch (Exception e) {
            log.error("계좌별 거래 내역 조회 실패 - 계좌 ID: {}", accountId, e);
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
     * 기간별 거래 내역 조회
     */
    public List<Transaction> getUserTransactionsByPeriod(Integer userId, int days) {
        try {
            log.debug("기간별 거래 내역 조회 - 사용자 ID: {}, 기간: {}일", userId, days);

            return transactionMapper.selectByUserIdAndPeriod(userId, days);

        } catch (Exception e) {
            log.error("기간별 거래 내역 조회 실패 - 사용자 ID: {}, 기간: {}", userId, days, e);
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
     * 거래 내역 통계 조회
     */
    public TransactionStatistics getTransactionStatistics(Integer userId, String period) {
        try {
            log.debug("거래 통계 조회 - 사용자 ID: {}, 기간: {}", userId, period);

            List<Transaction> transactions;

            // 기간별 거래 내역 조회
            switch (period.toLowerCase()) {
                case "week":
                    transactions = transactionMapper.selectByUserIdAndPeriod(userId, 7);
                    break;
                case "month":
                    transactions = transactionMapper.selectByUserIdAndPeriod(userId, 30);
                    break;
                case "year":
                    transactions = transactionMapper.selectByUserIdAndPeriod(userId, 365);
                    break;
                default:
                    transactions = transactionMapper.selectByUserId(userId, 1000, 0);
                    break;
            }

            // 통계 계산
            long totalBuyAmount = 0;
            long totalSellAmount = 0;
            int buyCount = 0;
            int sellCount = 0;

            for (Transaction transaction : transactions) {
                if ("BUY".equals(transaction.getTransactionType())) {
                    totalBuyAmount += transaction.getTotalAmount();
                    buyCount++;
                } else if ("SELL".equals(transaction.getTransactionType())) {
                    totalSellAmount += transaction.getTotalAmount();
                    sellCount++;
                }
            }

            return TransactionStatistics.builder()
                    .totalBuyAmount(totalBuyAmount)
                    .totalSellAmount(totalSellAmount)
                    .buyCount(buyCount)
                    .sellCount(sellCount)
                    .totalTransactions(buyCount + sellCount)
                    .netAmount(totalSellAmount - totalBuyAmount)
                    .build();

        } catch (Exception e) {
            log.error("거래 통계 조회 실패 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("거래 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 계좌 초기화 시 모든 거래 내역 삭제
     */
    @Transactional
    public void deleteAllTransactionsByAccount(Integer accountId) {
        try {
            log.info("계좌 초기화 - 모든 거래 내역 삭제, 계좌 ID: {}", accountId);
            int result = transactionMapper.deleteAllByAccount(accountId);
            log.info("삭제된 거래 내역 수: {}", result);

        } catch (Exception e) {
            log.error("거래 내역 일괄 삭제 실패 - 계좌 ID: {}", accountId, e);
            throw new RuntimeException("거래 내역 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 거래 횟수 조회
     */
    public int getTransactionCount(Integer userId) {
        try {
            return transactionMapper.countByUserId(userId);
        } catch (Exception e) {
            log.error("거래 횟수 조회 실패 - 사용자 ID: {}", userId, e);
            return 0;
        }
    }

    /**
     * 거래 타입별 총 금액 조회
     */
    public Long getTotalAmountByType(Integer userId, String transactionType, int days) {
        try {
            return transactionMapper.sumByUserIdAndType(userId, transactionType, days);
        } catch (Exception e) {
            log.error("거래 타입별 총 금액 조회 실패 - 사용자 ID: {}, 타입: {}", userId, transactionType, e);
            return 0L;
        }
    }

    /**
     * 거래 타입별 횟수 조회
     */
    public int getTransactionCountByType(Integer userId, String transactionType, int days) {
        try {
            return transactionMapper.countByUserIdAndType(userId, transactionType, days);
        } catch (Exception e) {
            log.error("거래 타입별 횟수 조회 실패 - 사용자 ID: {}, 타입: {}", userId, transactionType, e);
            return 0;
        }
    }

    /**
     * 특정 거래 내역 조회
     */
    public Transaction getTransactionById(Integer transactionId) {
        try {
            return transactionMapper.selectByTransactionId(transactionId);
        } catch (Exception e) {
            log.error("거래 내역 조회 실패 - 거래 ID: {}", transactionId, e);
            return null;
        }
    }

    /**
     * 매수/매도 가능 여부 사전 검증
     */
    public boolean validateOrder(Integer userId, String transactionType, String stockCode,
                                 Integer quantity, Integer price) {
        try {
            UserAccount account = userAccountService.getUserAccount(userId);
            if (account == null) {
                log.warn("계좌 정보를 찾을 수 없습니다 - 사용자 ID: {}", userId);
                return false;
            }

            if ("BUY".equals(transactionType)) {
                // 매수 시 잔고 확인
                long totalCost = (long) quantity * price;
                boolean canAfford = account.getCurrentBalance() >= totalCost;

                if (!canAfford) {
                    log.warn("매수 자금 부족 - 필요: {}, 보유: {}", totalCost, account.getCurrentBalance());
                }

                return canAfford;

            } else if ("SELL".equals(transactionType)) {
                // 매도 시 보유 수량 확인
                int holdingQuantity = holdingService.getHoldingQuantity(userId, stockCode);
                boolean canSell = holdingQuantity >= quantity;

                if (!canSell) {
                    log.warn("매도 수량 부족 - 필요: {}, 보유: {}", quantity, holdingQuantity);
                }

                return canSell;
            }

            return false;

        } catch (Exception e) {
            log.error("주문 검증 실패 - 사용자 ID: {}, 타입: {}", userId, transactionType, e);
            return false;
        }
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
     * 거래량 기반 인기 종목 조회 (최근 N일)
     */
    public List<PopularStock> getPopularStocksByVolume(int days, int limit) {
        try {
            log.debug("거래량 기반 인기 종목 조회 - 기간: {}일, 제한: {}", days, limit);

            // TODO: 별도 쿼리 구현 필요
            // 현재는 기본 거래 내역으로 대체
            List<Transaction> recentTransactions = transactionMapper.selectByUserIdAndPeriod(null, days);

            // 종목별 거래량 집계 로직 구현
            // Map을 사용하여 종목별 총 거래량 계산

            return null; // 임시

        } catch (Exception e) {
            log.error("인기 종목 조회 실패 - 기간: {}", days, e);
            return null;
        }
    }

    /**
     * 사용자의 평균 거래 단가 조회
     */
    public Double getAverageTransactionPrice(Integer userId, String stockCode) {
        try {
            List<Transaction> transactions = getUserTransactionsByStock(userId, stockCode, 1000, 0);

            if (transactions.isEmpty()) {
                return 0.0;
            }

            double weightedSum = 0.0;
            int totalQuantity = 0;

            for (Transaction transaction : transactions) {
                weightedSum += transaction.getPrice() * transaction.getQuantity();
                totalQuantity += transaction.getQuantity();
            }

            return totalQuantity > 0 ? weightedSum / totalQuantity : 0.0;

        } catch (Exception e) {
            log.error("평균 거래 단가 조회 실패 - 사용자 ID: {}, 종목: {}", userId, stockCode, e);
            return 0.0;
        }
    }

    /**
     * 거래 내역 통계 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransactionStatistics {
        private long totalBuyAmount;    // 총 매수 금액
        private long totalSellAmount;   // 총 매도 금액
        private int buyCount;           // 매수 횟수
        private int sellCount;          // 매도 횟수
        private int totalTransactions;  // 총 거래 횟수
        private long netAmount;         // 순 거래 금액 (매도 - 매수)
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

    /**
     * 인기 종목 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PopularStock {
        private String stockCode;      // 종목코드
        private String stockName;      // 종목명
        private long totalVolume;      // 총 거래량
        private long totalAmount;      // 총 거래금액
        private int transactionCount;  // 거래 횟수
        private double averagePrice;   // 평균 거래가
    }
}