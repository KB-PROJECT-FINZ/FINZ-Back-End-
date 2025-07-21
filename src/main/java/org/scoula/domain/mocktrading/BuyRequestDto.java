package org.scoula.domain.mocktrading;

/**
 * 가상 매수 요청을 위한 DTO
 */
public class BuyRequestDto {

    private String stockCode;   // 종목 코드 (예: "005930")
    private int quantity;       // 매수 수량
    private int price;          // 사용자가 설정한 매수가 (지정가)
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
