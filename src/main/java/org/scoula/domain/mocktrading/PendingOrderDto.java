package org.scoula.domain.mocktrading;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.sql.Timestamp;

/**
 * 체결 대기 중인 예약 주문 정보 DTO
 */
@ApiModel(description = "체결 대기 중인 예약 주문 정보")
public class PendingOrderDto {

    @ApiModelProperty(value = "주문 ID", example = "1")
    private int orderId;

    @ApiModelProperty(value = "계좌 ID", example = "4")
    private int accountId;

    @ApiModelProperty(value = "종목 코드", example = "005930")
    private String stockCode;

    @ApiModelProperty(value = "종목명", example = "삼성전자")
    private String stockName;

    @ApiModelProperty(value = "주문 유형", allowableValues = "BUY,SELL", example = "BUY")
    private String orderType;

    @ApiModelProperty(value = "주문 수량", example = "10")
    private int quantity;

    @ApiModelProperty(value = "목표 가격", example = "75000")
    private int targetPrice;

    @ApiModelProperty(value = "주문 생성 날짜", example = "2025-08-01T10:00:00.000")
    private Timestamp createdAt;

    public PendingOrderDto() {}

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getTargetPrice() { return targetPrice; }
    public void setTargetPrice(int targetPrice) { this.targetPrice = targetPrice; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}