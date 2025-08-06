package org.scoula.domain.mocktrading;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "시장가 주문 요청 정보")
public class MarketOrderRequestDto {
    @ApiModelProperty(value = "거래 유형", example = "BUY")
    private String transactionType;
    @ApiModelProperty(value = "주문 수량", example = "2")
    private int quantity;
    @ApiModelProperty(value = "종목 코드", example = "000660")
    private String stockCode;
    @ApiModelProperty(value = "종목명", example = "SK하이닉스")
    private String stockName;
    @ApiModelProperty(value = "시장가", example = "250000")
    private int marketPrice;

    // getters/setters
    public MarketOrderRequestDto() {}

    public String getTransactionType() {
        return transactionType;
    }
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStockCode() {
        return stockCode;
    }
    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockName() {
        return stockName;
    }
    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public int getMarketPrice() {
        return marketPrice;
    }
    public void setMarketPrice(int marketPrice) {
        this.marketPrice = marketPrice;
    }
}
