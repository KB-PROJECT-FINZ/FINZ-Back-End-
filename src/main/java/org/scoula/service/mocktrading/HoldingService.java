package org.scoula.service.mocktrading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.mocktrading.vo.Holding;
import org.scoula.mapper.trading.HoldingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingService {

    private final HoldingMapper holdingMapper;

    /**
     * 사용자의 모든 보유 종목 조회
     */
    public List<Holding> getUserHoldings(Integer userId) {
        try {
            log.debug("사용자 보유 종목 조회 - 사용자 ID: {}", userId);

            List<Holding> holdings = holdingMapper.selectByUserId(userId);

            // 현재가 업데이트 및 손익 계산
            for (Holding holding : holdings) {
                updateHoldingProfitLoss(holding);
            }

            return holdings;

        } catch (Exception e) {
            log.error("보유 종목 조회 실패 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("보유 종목 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 종목 보유 정보 조회
     */
    public Holding getHoldingByUserAndStock(Integer userId, String stockCode) {
        try {
            log.debug("특정 보유 종목 조회 - 사용자 ID: {}, 종목코드: {}", userId, stockCode);

            Holding holding = holdingMapper.selectByUserAndStock(userId, stockCode);
            if (holding != null) {
                updateHoldingProfitLoss(holding);
            }

            return holding;

        } catch (Exception e) {
            log.error("특정 보유 종목 조회 실패 - 사용자 ID: {}, 종목코드: {}", userId, stockCode, e);
            throw new RuntimeException("보유 종목 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 보유 종목 추가 또는 업데이트 (매수 시)
     */
    @Transactional
    public void addOrUpdateHolding(Integer accountId, String stockCode, String stockName,
                                   Integer quantity, Integer price) {
        try {
            log.info("보유 종목 추가/업데이트 - 계좌 ID: {}, 종목: {}, 수량: {}, 가격: {}",
                    accountId, stockCode, quantity, price);

            Holding existingHolding = holdingMapper.selectByAccountAndStock(accountId, stockCode);

            if (existingHolding == null) {
                // 새로운 보유 종목 추가
                Holding newHolding = Holding.builder()
                        .accountId(accountId)
                        .stockCode(stockCode)
                        .stockName(stockName)
                        .quantity(quantity)
                        .averagePrice(BigDecimal.valueOf(price))
                        .totalCost((long) quantity * price)
                        .currentPrice(price)
                        .build();

                // 손익 계산
                updateHoldingProfitLoss(newHolding);

                int result = holdingMapper.insertHolding(newHolding);
                if (result <= 0) {
                    throw new RuntimeException("보유 종목 추가에 실패했습니다.");
                }

                log.info("새로운 보유 종목 추가 완료 - 종목: {}, 보유 ID: {}", stockCode, newHolding.getHoldingId());

            } else {
                // 기존 보유 종목 업데이트 (평균 매수가 재계산)
                int newTotalQuantity = existingHolding.getQuantity() + quantity;
                long newTotalCost = existingHolding.getTotalCost() + ((long) quantity * price);
                BigDecimal newAveragePrice = BigDecimal.valueOf(newTotalCost)
                        .divide(BigDecimal.valueOf(newTotalQuantity), 2, RoundingMode.HALF_UP);

                existingHolding.setQuantity(newTotalQuantity);
                existingHolding.setAveragePrice(newAveragePrice);
                existingHolding.setTotalCost(newTotalCost);
                existingHolding.setCurrentPrice(price);

                // 손익 재계산
                updateHoldingProfitLoss(existingHolding);

                int result = holdingMapper.updateHolding(existingHolding);
                if (result <= 0) {
                    throw new RuntimeException("보유 종목 업데이트에 실패했습니다.");
                }

                log.info("보유 종목 업데이트 완료 - 종목: {}, 총수량: {}, 평균가: {}",
                        stockCode, newTotalQuantity, newAveragePrice);
            }

        } catch (Exception e) {
            log.error("보유 종목 추가/업데이트 실패", e);
            throw new RuntimeException("보유 종목 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 보유 종목 수량 감소 (매도 시)
     */
    @Transactional
    public boolean reduceHolding(Integer accountId, String stockCode, Integer quantity, Integer currentPrice) {
        try {
            log.info("보유 종목 수량 감소 - 계좌 ID: {}, 종목: {}, 매도수량: {}",
                    accountId, stockCode, quantity);

            Holding holding = holdingMapper.selectByAccountAndStock(accountId, stockCode);
            if (holding == null) {
                log.warn("보유하지 않은 종목 매도 시도 - 계좌 ID: {}, 종목: {}", accountId, stockCode);
                return false;
            }

            if (holding.getQuantity() < quantity) {
                log.warn("보유 수량 부족 - 보유: {}, 매도요청: {}", holding.getQuantity(), quantity);
                return false;
            }

            int remainingQuantity = holding.getQuantity() - quantity;

            if (remainingQuantity == 0) {
                // 전량 매도 - 보유 종목 삭제
                int result = holdingMapper.deleteHolding(holding.getHoldingId());
                if (result <= 0) {
                    throw new RuntimeException("보유 종목 삭제에 실패했습니다.");
                }
                log.info("전량 매도 완료 - 종목: {} 삭제", stockCode);
            } else {
                // 일부 매도 - 수량 및 총 매수금액 조정
                long soldCost = (long) quantity * holding.getAveragePrice().intValue();
                long newTotalCost = holding.getTotalCost() - soldCost;

                holding.setQuantity(remainingQuantity);
                holding.setTotalCost(newTotalCost);
                holding.setCurrentPrice(currentPrice);

                // 손익 재계산
                updateHoldingProfitLoss(holding);

                int result = holdingMapper.updateHolding(holding);
                if (result <= 0) {
                    throw new RuntimeException("보유 종목 업데이트에 실패했습니다.");
                }
                log.info("일부 매도 완료 - 종목: {}, 남은수량: {}", stockCode, remainingQuantity);
            }

            return true;

        } catch (Exception e) {
            log.error("보유 종목 수량 감소 실패", e);
            throw new RuntimeException("보유 종목 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 보유 종목의 현재 손익 계산 및 업데이트
     */
    private void updateHoldingProfitLoss(Holding holding) {
        if (holding.getCurrentPrice() != null && holding.getQuantity() > 0) {
            // 현재 평가금액 = 현재가 × 보유수량
            long currentValue = (long) holding.getCurrentPrice() * holding.getQuantity();
            holding.setCurrentValue(currentValue);

            // 평가 손익 = 현재평가금액 - 총매수금액
            long profitLoss = currentValue - holding.getTotalCost();
            holding.setProfitLoss(profitLoss);

            // 수익률 = (평가손익 ÷ 총매수금액) × 100
            if (holding.getTotalCost() > 0) {
                BigDecimal profitRate = BigDecimal.valueOf(profitLoss)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(holding.getTotalCost()), 2, RoundingMode.HALF_UP);
                holding.setProfitRate(profitRate);
            } else {
                holding.setProfitRate(BigDecimal.ZERO);
            }
        } else {
            // 현재가 정보가 없는 경우 기본값 설정
            holding.setCurrentValue(0L);
            holding.setProfitLoss(0L);
            holding.setProfitRate(BigDecimal.ZERO);
        }
    }


    /**
     * 특정 종목의 보유 수량 조회
     */
    public int getHoldingQuantity(Integer userId, String stockCode) {
        try {
            Holding holding = getHoldingByUserAndStock(userId, stockCode);
            return holding != null ? holding.getQuantity() : 0;
        } catch (Exception e) {
            log.error("보유 수량 조회 실패 - 사용자 ID: {}, 종목: {}", userId, stockCode, e);
            return 0;
        }
    }

    /**
     * 보유 종목별 비중 계산
     */
    public List<Holding> getHoldingsWithPercentage(Integer userId, Long totalAssetValue) {
        try {
            List<Holding> holdings = getUserHoldings(userId);

            if (totalAssetValue > 0) {
                for (Holding holding : holdings) {
                    if (holding.getCurrentValue() != null) {
                        int percentage = Math.round((holding.getCurrentValue() * 100.0f) / totalAssetValue);
                        holding.setPercentage(percentage);
                    } else {
                        holding.setPercentage(0);
                    }
                }
            } else {
                holdings.forEach(h -> h.setPercentage(0));
            }

            return holdings;

        } catch (Exception e) {
            log.error("보유 종목 비중 계산 실패 - 사용자 ID: {}", userId, e);
            return getUserHoldings(userId); // 비중 없이 반환
        }
    }
}