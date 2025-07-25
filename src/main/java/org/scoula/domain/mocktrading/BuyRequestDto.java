package org.scoula.domain.mocktrading;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 가상 매수 요청을 위한 DTO
 */
@ApiModel(description = "주식 매수 요청 정보")
public class BuyRequestDto {

    @ApiModelProperty(value = "종목 코드", required = true, example = "005930", notes = "삼성전자의 경우 005930")
    private String stockCode;   // 종목 코드 (예: "005930")
    
    @ApiModelProperty(value = "매수 수량", required = true, example = "10", notes = "구매할 주식 수량")
    private int quantity;       // 매수 수량
    
    @ApiModelProperty(value = "매수 가격", required = true, example = "75000", notes = "원하는 매수 가격 (원)")
    private int price;          // 사용자가 설정한 매수가 (지정가)
    
    @ApiModelProperty(value = "주문 유형", required = true, example = "LIMIT", allowableValues = "LIMIT,MARKET", notes = "LIMIT: 지정가, MARKET: 시장가")
    private String orderType;   // 주문 유형: "LIMIT" or "MARKET"

    public BuyRequestDto() {
    }

    public BuyRequestDto(String stockCode, int quantity, int price, String orderType) {
        this.stockCode = stockCode;
        this.quantity = quantity;
        this.price = price;
        this.orderType = orderType;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
}
