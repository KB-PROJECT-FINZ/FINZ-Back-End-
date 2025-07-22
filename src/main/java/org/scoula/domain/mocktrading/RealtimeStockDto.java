package org.scoula.domain.mocktrading;

public class RealtimeStockDto {

    private String stockCode;
    private String stockName;
    private String price;
    private String sign;
    private String change;
    private String rate;
    private String volume;
    private String value;
    private String high;
    private String low;

    // ✅ 기본 생성자
    public RealtimeStockDto() {}

    // ✅ 전체 필드 초기화 생성자
    public RealtimeStockDto(String stockCode, String stockName, String price, String sign,
                            String change, String rate, String volume, String value,
                            String high, String low) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.price = price;
        this.sign = sign;
        this.change = change;
        this.rate = rate;
        this.volume = volume;
        this.value = value;
        this.high = high;
        this.low = low;
    }

    // ✅ getter 메서드들
    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
    public String getPrice() { return price; }
    public String getSign() { return sign; }
    public String getChange() { return change; }
    public String getRate() { return rate; }
    public String getVolume() { return volume; }
    public String getValue() { return value; }
    public String getHigh() { return high; }
    public String getLow() { return low; }
}
