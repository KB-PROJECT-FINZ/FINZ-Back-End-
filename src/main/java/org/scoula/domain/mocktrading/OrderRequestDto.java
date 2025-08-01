package org.scoula.domain.mocktrading;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 매수/매도 주문 요청 DTO (accountId 제거)
 */
@ApiModel(description = "매수/매도 주문 요청 정보")
public class OrderRequestDto {

    @ApiModelProperty(value = "종목 코드", example = "005930")
    private String stockCode;

    @ApiModelProperty(value = "종목명", example = "삼성전자")
    private String stockName;

    @ApiModelProperty(value = "주문 유형", allowableValues = "BUY,SELL", example = "BUY")
    private String orderType; // "BUY" or "SELL"

    @ApiModelProperty(value = "주문 수량", example = "10")
    private int quantity;

    @ApiModelProperty(value = "목표 가격", example = "75000")
    private int targetPrice;

    public OrderRequestDto() {}

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
}