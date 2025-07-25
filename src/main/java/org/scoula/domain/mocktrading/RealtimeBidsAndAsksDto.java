package org.scoula.domain.mocktrading;

public class RealtimeBidsAndAsksDto {

    // 기본 정보
    private String stockCode;           // MKSC_SHRN_ISCD - 유가증권 단축 종목코드
    private String businessHour;        // BSOP_HOUR - 영업 시간
    private String hourCode;            // HOUR_CLS_CODE - 시간 구분 코드

    // 매도호가 (1~10)
    private String askPrice1;           // ASKP1 - 매도호가1
    private String askPrice2;           // ASKP2 - 매도호가2
    private String askPrice3;           // ASKP3 - 매도호가3
    private String askPrice4;           // ASKP4 - 매도호가4
    private String askPrice5;           // ASKP5 - 매도호가5
    private String askPrice6;           // ASKP6 - 매도호가6
    private String askPrice7;           // ASKP7 - 매도호가7
    private String askPrice8;           // ASKP8 - 매도호가8
    private String askPrice9;           // ASKP9 - 매도호가9
    private String askPrice10;          // ASKP10 - 매도호가10

    // 매수호가 (1~10)
    private String bidPrice1;           // BIDP1 - 매수호가1
    private String bidPrice2;           // BIDP2 - 매수호가2
    private String bidPrice3;           // BIDP3 - 매수호가3
    private String bidPrice4;           // BIDP4 - 매수호가4
    private String bidPrice5;           // BIDP5 - 매수호가5
    private String bidPrice6;           // BIDP6 - 매수호가6
    private String bidPrice7;           // BIDP7 - 매수호가7
    private String bidPrice8;           // BIDP8 - 매수호가8
    private String bidPrice9;           // BIDP9 - 매수호가9
    private String bidPrice10;          // BIDP10 - 매수호가10

    // 매도호가 잔량 (1~10)
    private String askQty1;             // ASKP_RSQN1 - 매도호가 잔량1
    private String askQty2;             // ASKP_RSQN2 - 매도호가 잔량2
    private String askQty3;             // ASKP_RSQN3 - 매도호가 잔량3
    private String askQty4;             // ASKP_RSQN4 - 매도호가 잔량4
    private String askQty5;             // ASKP_RSQN5 - 매도호가 잔량5
    private String askQty6;             // ASKP_RSQN6 - 매도호가 잔량6
    private String askQty7;             // ASKP_RSQN7 - 매도호가 잔량7
    private String askQty8;             // ASKP_RSQN8 - 매도호가 잔량8
    private String askQty9;             // ASKP_RSQN9 - 매도호가 잔량9
    private String askQty10;            // ASKP_RSQN10 - 매도호가 잔량10

    // 매수호가 잔량 (1~10)
    private String bidQty1;             // BIDP_RSQN1 - 매수호가 잔량1
    private String bidQty2;             // BIDP_RSQN2 - 매수호가 잔량2
    private String bidQty3;             // BIDP_RSQN3 - 매수호가 잔량3
    private String bidQty4;             // BIDP_RSQN4 - 매수호가 잔량4
    private String bidQty5;             // BIDP_RSQN5 - 매수호가 잔량5
    private String bidQty6;             // BIDP_RSQN6 - 매수호가 잔량6
    private String bidQty7;             // BIDP_RSQN7 - 매수호가 잔량7
    private String bidQty8;             // BIDP_RSQN8 - 매수호가 잔량8
    private String bidQty9;             // BIDP_RSQN9 - 매수호가 잔량9
    private String bidQty10;            // BIDP_RSQN10 - 매수호가 잔량10

    // 총 잔량 정보
    private String totalAskQty;         // TOTAL_ASKP_RSQN - 총 매도호가 잔량
    private String totalBidQty;         // TOTAL_BIDP_RSQN - 총 매수호가 잔량
    private String overtimeAskQty;      // OVTM_TOTAL_ASKP_RSQN - 시간외 총 매도호가 잔량
    private String overtimeBidQty;      // OVTM_TOTAL_BIDP_RSQN - 시간외 총 매수호가 잔량

    // 예상 체결 정보
    private String expectedPrice;       // ANTC_CNPR - 예상 체결가
    private String expectedQty;         // ANTC_CNQN - 예상 체결량
    private String expectedVolume;      // ANTC_VOL - 예상 거래량
    private String expectedDiff;        // ANTC_CNTG_VRSS - 예상 체결 대비
    private String expectedSign;        // ANTC_CNTG_VRSS_SIGN - 예상 체결 대비 부호
    private String expectedRate;        // ANTC_CNTG_PRDY_CTRT - 예상 체결 전일 대비율

    // 기타 정보
    private String accumulatedVolume;   // ACML_VOL - 누적 거래량
    private String askQtyChange;        // TOTAL_ASKP_RSQN_ICDC - 총 매도호가 잔량 증감
    private String bidQtyChange;        // TOTAL_BIDP_RSQN_ICDC - 총 매수호가 잔량 증감
    private String overtimeAskChange;   // OVTM_TOTAL_ASKP_ICDC - 시간외 총 매도호가 증감
    private String overtimeBidChange;   // OVTM_TOTAL_BIDP_ICDC - 시간외 총 매수호가 증감

    // ✅ 기본 생성자
    public RealtimeBidsAndAsksDto() {}

    // ✅ 편의 메서드들
    public String getHourCodeDescription() {
        switch (hourCode) {
            case "0": return "장중";
            case "A": return "장후예상";
            case "B": return "장전예상";
            case "C": return "9시이후 예상가/VI발동";
            case "D": return "시간외 단일가 예상";
            default: return "기타";
        }
    }

    public String getExpectedSignDescription() {
        switch (expectedSign) {
            case "1": return "상한";
            case "2": return "상승";
            case "3": return "보합";
            case "4": return "하한";
            case "5": return "하락";
            default: return "기타";
        }
    }

    // ✅ Getter/Setter 메서드들 (모든 필드)
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    public String getBusinessHour() { return businessHour; }
    public void setBusinessHour(String businessHour) { this.businessHour = businessHour; }

    public String getHourCode() { return hourCode; }
    public void setHourCode(String hourCode) { this.hourCode = hourCode; }

    // 매도호가 getter/setter
    public String getAskPrice1() { return askPrice1; }
    public void setAskPrice1(String askPrice1) { this.askPrice1 = askPrice1; }
    public String getAskPrice2() { return askPrice2; }
    public void setAskPrice2(String askPrice2) { this.askPrice2 = askPrice2; }
    public String getAskPrice3() { return askPrice3; }
    public void setAskPrice3(String askPrice3) { this.askPrice3 = askPrice3; }
    public String getAskPrice4() { return askPrice4; }
    public void setAskPrice4(String askPrice4) { this.askPrice4 = askPrice4; }
    public String getAskPrice5() { return askPrice5; }
    public void setAskPrice5(String askPrice5) { this.askPrice5 = askPrice5; }
    public String getAskPrice6() { return askPrice6; }
    public void setAskPrice6(String askPrice6) { this.askPrice6 = askPrice6; }
    public String getAskPrice7() { return askPrice7; }
    public void setAskPrice7(String askPrice7) { this.askPrice7 = askPrice7; }
    public String getAskPrice8() { return askPrice8; }
    public void setAskPrice8(String askPrice8) { this.askPrice8 = askPrice8; }
    public String getAskPrice9() { return askPrice9; }
    public void setAskPrice9(String askPrice9) { this.askPrice9 = askPrice9; }
    public String getAskPrice10() { return askPrice10; }
    public void setAskPrice10(String askPrice10) { this.askPrice10 = askPrice10; }

    // 매수호가 getter/setter
    public String getBidPrice1() { return bidPrice1; }
    public void setBidPrice1(String bidPrice1) { this.bidPrice1 = bidPrice1; }
    public String getBidPrice2() { return bidPrice2; }
    public void setBidPrice2(String bidPrice2) { this.bidPrice2 = bidPrice2; }
    public String getBidPrice3() { return bidPrice3; }
    public void setBidPrice3(String bidPrice3) { this.bidPrice3 = bidPrice3; }
    public String getBidPrice4() { return bidPrice4; }
    public void setBidPrice4(String bidPrice4) { this.bidPrice4 = bidPrice4; }
    public String getBidPrice5() { return bidPrice5; }
    public void setBidPrice5(String bidPrice5) { this.bidPrice5 = bidPrice5; }
    public String getBidPrice6() { return bidPrice6; }
    public void setBidPrice6(String bidPrice6) { this.bidPrice6 = bidPrice6; }
    public String getBidPrice7() { return bidPrice7; }
    public void setBidPrice7(String bidPrice7) { this.bidPrice7 = bidPrice7; }
    public String getBidPrice8() { return bidPrice8; }
    public void setBidPrice8(String bidPrice8) { this.bidPrice8 = bidPrice8; }
    public String getBidPrice9() { return bidPrice9; }
    public void setBidPrice9(String bidPrice9) { this.bidPrice9 = bidPrice9; }
    public String getBidPrice10() { return bidPrice10; }
    public void setBidPrice10(String bidPrice10) { this.bidPrice10 = bidPrice10; }

    // 매도잔량 getter/setter
    public String getAskQty1() { return askQty1; }
    public void setAskQty1(String askQty1) { this.askQty1 = askQty1; }
    public String getAskQty2() { return askQty2; }
    public void setAskQty2(String askQty2) { this.askQty2 = askQty2; }
    public String getAskQty3() { return askQty3; }
    public void setAskQty3(String askQty3) { this.askQty3 = askQty3; }
    public String getAskQty4() { return askQty4; }
    public void setAskQty4(String askQty4) { this.askQty4 = askQty4; }
    public String getAskQty5() { return askQty5; }
    public void setAskQty5(String askQty5) { this.askQty5 = askQty5; }
    public String getAskQty6() { return askQty6; }
    public void setAskQty6(String askQty6) { this.askQty6 = askQty6; }
    public String getAskQty7() { return askQty7; }
    public void setAskQty7(String askQty7) { this.askQty7 = askQty7; }
    public String getAskQty8() { return askQty8; }
    public void setAskQty8(String askQty8) { this.askQty8 = askQty8; }
    public String getAskQty9() { return askQty9; }
    public void setAskQty9(String askQty9) { this.askQty9 = askQty9; }
    public String getAskQty10() { return askQty10; }
    public void setAskQty10(String askQty10) { this.askQty10 = askQty10; }

    // 매수잔량 getter/setter
    public String getBidQty1() { return bidQty1; }
    public void setBidQty1(String bidQty1) { this.bidQty1 = bidQty1; }
    public String getBidQty2() { return bidQty2; }
    public void setBidQty2(String bidQty2) { this.bidQty2 = bidQty2; }
    public String getBidQty3() { return bidQty3; }
    public void setBidQty3(String bidQty3) { this.bidQty3 = bidQty3; }
    public String getBidQty4() { return bidQty4; }
    public void setBidQty4(String bidQty4) { this.bidQty4 = bidQty4; }
    public String getBidQty5() { return bidQty5; }
    public void setBidQty5(String bidQty5) { this.bidQty5 = bidQty5; }
    public String getBidQty6() { return bidQty6; }
    public void setBidQty6(String bidQty6) { this.bidQty6 = bidQty6; }
    public String getBidQty7() { return bidQty7; }
    public void setBidQty7(String bidQty7) { this.bidQty7 = bidQty7; }
    public String getBidQty8() { return bidQty8; }
    public void setBidQty8(String bidQty8) { this.bidQty8 = bidQty8; }
    public String getBidQty9() { return bidQty9; }
    public void setBidQty9(String bidQty9) { this.bidQty9 = bidQty9; }
    public String getBidQty10() { return bidQty10; }
    public void setBidQty10(String bidQty10) { this.bidQty10 = bidQty10; }

    // 나머지 필드들 getter/setter
    public String getTotalAskQty() { return totalAskQty; }
    public void setTotalAskQty(String totalAskQty) { this.totalAskQty = totalAskQty; }

    public String getTotalBidQty() { return totalBidQty; }
    public void setTotalBidQty(String totalBidQty) { this.totalBidQty = totalBidQty; }

    public String getOvertimeAskQty() { return overtimeAskQty; }
    public void setOvertimeAskQty(String overtimeAskQty) { this.overtimeAskQty = overtimeAskQty; }

    public String getOvertimeBidQty() { return overtimeBidQty; }
    public void setOvertimeBidQty(String overtimeBidQty) { this.overtimeBidQty = overtimeBidQty; }

    public String getExpectedPrice() { return expectedPrice; }
    public void setExpectedPrice(String expectedPrice) { this.expectedPrice = expectedPrice; }

    public String getExpectedQty() { return expectedQty; }
    public void setExpectedQty(String expectedQty) { this.expectedQty = expectedQty; }

    public String getExpectedVolume() { return expectedVolume; }
    public void setExpectedVolume(String expectedVolume) { this.expectedVolume = expectedVolume; }

    public String getExpectedDiff() { return expectedDiff; }
    public void setExpectedDiff(String expectedDiff) { this.expectedDiff = expectedDiff; }

    public String getExpectedSign() { return expectedSign; }
    public void setExpectedSign(String expectedSign) { this.expectedSign = expectedSign; }

    public String getExpectedRate() { return expectedRate; }
    public void setExpectedRate(String expectedRate) { this.expectedRate = expectedRate; }

    public String getAccumulatedVolume() { return accumulatedVolume; }
    public void setAccumulatedVolume(String accumulatedVolume) { this.accumulatedVolume = accumulatedVolume; }

    public String getAskQtyChange() { return askQtyChange; }
    public void setAskQtyChange(String askQtyChange) { this.askQtyChange = askQtyChange; }

    public String getBidQtyChange() { return bidQtyChange; }
    public void setBidQtyChange(String bidQtyChange) { this.bidQtyChange = bidQtyChange; }

    public String getOvertimeAskChange() { return overtimeAskChange; }
    public void setOvertimeAskChange(String overtimeAskChange) { this.overtimeAskChange = overtimeAskChange; }

    public String getOvertimeBidChange() { return overtimeBidChange; }
    public void setOvertimeBidChange(String overtimeBidChange) { this.overtimeBidChange = overtimeBidChange; }
}