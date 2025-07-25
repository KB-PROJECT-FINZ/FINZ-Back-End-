package org.scoula.domain.mocktrading;

public class RealtimeStockDto {
    
    // 기본 정보
    private String stockCode;           // MKSC_SHRN_ISCD - 유가증권 단축 종목코드
    private String contractTime;        // STCK_CNTG_HOUR - 주식 체결 시간
    private String currentPrice;        // STCK_PRPR - 주식 현재가
    private String prevDaySign;         // PRDY_VRSS_SIGN - 전일 대비 부호
    private String prevDayDiff;         // PRDY_VRSS - 전일 대비
    private String prevDayRate;         // PRDY_CTRT - 전일 대비율
    private String weightedAvgPrice;    // WGHN_AVRG_STCK_PRC - 가중 평균 주식 가격
    
    // 시가/고가/저가
    private String openPrice;           // STCK_OPRC - 주식 시가
    private String highPrice;           // STCK_HGPR - 주식 최고가
    private String lowPrice;            // STCK_LWPR - 주식 최저가
    
    // 호가 정보
    private String askPrice1;           // ASKP1 - 매도호가1
    private String bidPrice1;           // BIDP1 - 매수호가1
    
    // 거래량 정보
    private String contractVolume;      // CNTG_VOL - 체결 거래량
    private String accumulatedVolume;   // ACML_VOL - 누적 거래량
    private String accumulatedAmount;   // ACML_TR_PBMN - 누적 거래 대금
    
    // 체결 건수
    private String sellContractCount;   // SELN_CNTG_CSNU - 매도 체결 건수
    private String buyContractCount;    // SHNU_CNTG_CSNU - 매수 체결 건수
    private String netBuyCount;         // NTBY_CNTG_CSNU - 순매수 체결 건수
    
    // 체결 정보
    private String contractIntensity;   // CTTR - 체결강도
    private String totalSellVolume;     // SELN_CNTG_SMTN - 총 매도 수량
    private String totalBuyVolume;      // SHNU_CNTG_SMTN - 총 매수 수량
    private String contractType;        // CCLD_DVSN - 체결구분
    private String buyRate;             // SHNU_RATE - 매수비율
    private String volumeRate;          // PRDY_VOL_VRSS_ACML_VOL_RATE - 전일 거래량 대비 등락율
    
    // 시간별 정보
    private String openTime;            // OPRC_HOUR - 시가 시간
    private String openVsCurrentSign;   // OPRC_VRSS_PRPR_SIGN - 시가대비구분
    private String openVsCurrentDiff;   // OPRC_VRSS_PRPR - 시가대비
    private String highTime;            // HGPR_HOUR - 최고가 시간
    private String highVsCurrentSign;   // HGPR_VRSS_PRPR_SIGN - 고가대비구분
    private String highVsCurrentDiff;   // HGPR_VRSS_PRPR - 고가대비
    private String lowTime;             // LWPR_HOUR - 최저가 시간
    private String lowVsCurrentSign;    // LWPR_VRSS_PRPR_SIGN - 저가대비구분
    private String lowVsCurrentDiff;    // LWPR_VRSS_PRPR - 저가대비
    
    // 기타 정보
    private String businessDate;        // BSOP_DATE - 영업 일자
    private String marketOperationCode; // NEW_MKOP_CLS_CODE - 신 장운영 구분 코드
    private String tradeHaltYn;         // TRHT_YN - 거래정지 여부
    private String askRemainQty1;       // ASKP_RSQN1 - 매도호가 잔량1
    private String bidRemainQty1;       // BIDP_RSQN1 - 매수호가 잔량1
    private String totalAskRemainQty;   // TOTAL_ASKP_RSQN - 총 매도호가 잔량
    private String totalBidRemainQty;   // TOTAL_BIDP_RSQN - 총 매수호가 잔량
    private String volumeTurnoverRate;  // VOL_TNRT - 거래량 회전율
    private String prevSameTimeVolume;  // PRDY_SMNS_HOUR_ACML_VOL - 전일 동시간 누적 거래량
    private String prevSameTimeRate;    // PRDY_SMNS_HOUR_ACML_VOL_RATE - 전일 동시간 누적 거래량 비율
    private String hourCode;            // HOUR_CLS_CODE - 시간 구분 코드
    private String marketCloseCode;     // MRKT_TRTM_CLS_CODE - 임의종료구분코드
    private String viStandardPrice;     // VI_STND_PRC - 정적VI발동기준가

    // ✅ 기본 생성자
    public RealtimeStockDto() {}

    // ✅ 모든 필드 생성자
    public RealtimeStockDto(String stockCode, String contractTime, String currentPrice, 
                           String prevDaySign, String prevDayDiff, String prevDayRate,
                           String weightedAvgPrice, String openPrice, String highPrice, 
                           String lowPrice, String askPrice1, String bidPrice1,
                           String contractVolume, String accumulatedVolume, String accumulatedAmount,
                           String sellContractCount, String buyContractCount, String netBuyCount,
                           String contractIntensity, String totalSellVolume, String totalBuyVolume,
                           String contractType, String buyRate, String volumeRate,
                           String openTime, String openVsCurrentSign, String openVsCurrentDiff,
                           String highTime, String highVsCurrentSign, String highVsCurrentDiff,
                           String lowTime, String lowVsCurrentSign, String lowVsCurrentDiff,
                           String businessDate, String marketOperationCode, String tradeHaltYn,
                           String askRemainQty1, String bidRemainQty1, String totalAskRemainQty,
                           String totalBidRemainQty, String volumeTurnoverRate, String prevSameTimeVolume,
                           String prevSameTimeRate, String hourCode, String marketCloseCode, String viStandardPrice) {
        this.stockCode = stockCode;
        this.contractTime = contractTime;
        this.currentPrice = currentPrice;
        this.prevDaySign = prevDaySign;
        this.prevDayDiff = prevDayDiff;
        this.prevDayRate = prevDayRate;
        this.weightedAvgPrice = weightedAvgPrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.askPrice1 = askPrice1;
        this.bidPrice1 = bidPrice1;
        this.contractVolume = contractVolume;
        this.accumulatedVolume = accumulatedVolume;
        this.accumulatedAmount = accumulatedAmount;
        this.sellContractCount = sellContractCount;
        this.buyContractCount = buyContractCount;
        this.netBuyCount = netBuyCount;
        this.contractIntensity = contractIntensity;
        this.totalSellVolume = totalSellVolume;
        this.totalBuyVolume = totalBuyVolume;
        this.contractType = contractType;
        this.buyRate = buyRate;
        this.volumeRate = volumeRate;
        this.openTime = openTime;
        this.openVsCurrentSign = openVsCurrentSign;
        this.openVsCurrentDiff = openVsCurrentDiff;
        this.highTime = highTime;
        this.highVsCurrentSign = highVsCurrentSign;
        this.highVsCurrentDiff = highVsCurrentDiff;
        this.lowTime = lowTime;
        this.lowVsCurrentSign = lowVsCurrentSign;
        this.lowVsCurrentDiff = lowVsCurrentDiff;
        this.businessDate = businessDate;
        this.marketOperationCode = marketOperationCode;
        this.tradeHaltYn = tradeHaltYn;
        this.askRemainQty1 = askRemainQty1;
        this.bidRemainQty1 = bidRemainQty1;
        this.totalAskRemainQty = totalAskRemainQty;
        this.totalBidRemainQty = totalBidRemainQty;
        this.volumeTurnoverRate = volumeTurnoverRate;
        this.prevSameTimeVolume = prevSameTimeVolume;
        this.prevSameTimeRate = prevSameTimeRate;
        this.hourCode = hourCode;
        this.marketCloseCode = marketCloseCode;
        this.viStandardPrice = viStandardPrice;
    }

    // ✅ 편의 메서드들
    public String getContractTypeDescription() {
        switch (contractType) {
            case "1": return "매수";
            case "3": return "장전";
            case "5": return "매도";
            default: return "기타";
        }
    }

    public String getPrevDaySignDescription() {
        switch (prevDaySign) {
            case "1": return "상한";
            case "2": return "상승";
            case "3": return "보합";
            case "4": return "하한";
            case "5": return "하락";
            default: return "기타";
        }
    }

    // ✅ Getter 메서드들 (모든 필드)
    public String getStockCode() { return stockCode; }
    public String getContractTime() { return contractTime; }
    public String getCurrentPrice() { return currentPrice; }
    public String getPrevDaySign() { return prevDaySign; }
    public String getPrevDayDiff() { return prevDayDiff; }
    public String getPrevDayRate() { return prevDayRate; }
    public String getWeightedAvgPrice() { return weightedAvgPrice; }
    public String getOpenPrice() { return openPrice; }
    public String getHighPrice() { return highPrice; }
    public String getLowPrice() { return lowPrice; }
    public String getAskPrice1() { return askPrice1; }
    public String getBidPrice1() { return bidPrice1; }
    public String getContractVolume() { return contractVolume; }
    public String getAccumulatedVolume() { return accumulatedVolume; }
    public String getAccumulatedAmount() { return accumulatedAmount; }
    public String getSellContractCount() { return sellContractCount; }
    public String getBuyContractCount() { return buyContractCount; }
    public String getNetBuyCount() { return netBuyCount; }
    public String getContractIntensity() { return contractIntensity; }
    public String getTotalSellVolume() { return totalSellVolume; }
    public String getTotalBuyVolume() { return totalBuyVolume; }
    public String getContractType() { return contractType; }
    public String getBuyRate() { return buyRate; }
    public String getVolumeRate() { return volumeRate; }
    public String getOpenTime() { return openTime; }
    public String getOpenVsCurrentSign() { return openVsCurrentSign; }
    public String getOpenVsCurrentDiff() { return openVsCurrentDiff; }
    public String getHighTime() { return highTime; }
    public String getHighVsCurrentSign() { return highVsCurrentSign; }
    public String getHighVsCurrentDiff() { return highVsCurrentDiff; }
    public String getLowTime() { return lowTime; }
    public String getLowVsCurrentSign() { return lowVsCurrentSign; }
    public String getLowVsCurrentDiff() { return lowVsCurrentDiff; }
    public String getBusinessDate() { return businessDate; }
    public String getMarketOperationCode() { return marketOperationCode; }
    public String getTradeHaltYn() { return tradeHaltYn; }
    public String getAskRemainQty1() { return askRemainQty1; }
    public String getBidRemainQty1() { return bidRemainQty1; }
    public String getTotalAskRemainQty() { return totalAskRemainQty; }
    public String getTotalBidRemainQty() { return totalBidRemainQty; }
    public String getVolumeTurnoverRate() { return volumeTurnoverRate; }
    public String getPrevSameTimeVolume() { return prevSameTimeVolume; }
    public String getPrevSameTimeRate() { return prevSameTimeRate; }
    public String getHourCode() { return hourCode; }
    public String getMarketCloseCode() { return marketCloseCode; }
    public String getViStandardPrice() { return viStandardPrice; }

    // ✅ Setter 메서드들 (JSON 파싱용)
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public void setContractTime(String contractTime) { this.contractTime = contractTime; }
    public void setCurrentPrice(String currentPrice) { this.currentPrice = currentPrice; }
    public void setPrevDaySign(String prevDaySign) { this.prevDaySign = prevDaySign; }
    public void setPrevDayDiff(String prevDayDiff) { this.prevDayDiff = prevDayDiff; }
    public void setPrevDayRate(String prevDayRate) { this.prevDayRate = prevDayRate; }
    public void setWeightedAvgPrice(String weightedAvgPrice) { this.weightedAvgPrice = weightedAvgPrice; }
    public void setOpenPrice(String openPrice) { this.openPrice = openPrice; }
    public void setHighPrice(String highPrice) { this.highPrice = highPrice; }
    public void setLowPrice(String lowPrice) { this.lowPrice = lowPrice; }
    public void setAskPrice1(String askPrice1) { this.askPrice1 = askPrice1; }
    public void setBidPrice1(String bidPrice1) { this.bidPrice1 = bidPrice1; }
    public void setContractVolume(String contractVolume) { this.contractVolume = contractVolume; }
    public void setAccumulatedVolume(String accumulatedVolume) { this.accumulatedVolume = accumulatedVolume; }
    public void setAccumulatedAmount(String accumulatedAmount) { this.accumulatedAmount = accumulatedAmount; }
    public void setSellContractCount(String sellContractCount) { this.sellContractCount = sellContractCount; }
    public void setBuyContractCount(String buyContractCount) { this.buyContractCount = buyContractCount; }
    public void setNetBuyCount(String netBuyCount) { this.netBuyCount = netBuyCount; }
    public void setContractIntensity(String contractIntensity) { this.contractIntensity = contractIntensity; }
    public void setTotalSellVolume(String totalSellVolume) { this.totalSellVolume = totalSellVolume; }
    public void setTotalBuyVolume(String totalBuyVolume) { this.totalBuyVolume = totalBuyVolume; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public void setBuyRate(String buyRate) { this.buyRate = buyRate; }
    public void setVolumeRate(String volumeRate) { this.volumeRate = volumeRate; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public void setOpenVsCurrentSign(String openVsCurrentSign) { this.openVsCurrentSign = openVsCurrentSign; }
    public void setOpenVsCurrentDiff(String openVsCurrentDiff) { this.openVsCurrentDiff = openVsCurrentDiff; }
    public void setHighTime(String highTime) { this.highTime = highTime; }
    public void setHighVsCurrentSign(String highVsCurrentSign) { this.highVsCurrentSign = highVsCurrentSign; }
    public void setHighVsCurrentDiff(String highVsCurrentDiff) { this.highVsCurrentDiff = highVsCurrentDiff; }
    public void setLowTime(String lowTime) { this.lowTime = lowTime; }
    public void setLowVsCurrentSign(String lowVsCurrentSign) { this.lowVsCurrentSign = lowVsCurrentSign; }
    public void setLowVsCurrentDiff(String lowVsCurrentDiff) { this.lowVsCurrentDiff = lowVsCurrentDiff; }
    public void setBusinessDate(String businessDate) { this.businessDate = businessDate; }
    public void setMarketOperationCode(String marketOperationCode) { this.marketOperationCode = marketOperationCode; }
    public void setTradeHaltYn(String tradeHaltYn) { this.tradeHaltYn = tradeHaltYn; }
    public void setAskRemainQty1(String askRemainQty1) { this.askRemainQty1 = askRemainQty1; }
    public void setBidRemainQty1(String bidRemainQty1) { this.bidRemainQty1 = bidRemainQty1; }
    public void setTotalAskRemainQty(String totalAskRemainQty) { this.totalAskRemainQty = totalAskRemainQty; }
    public void setTotalBidRemainQty(String totalBidRemainQty) { this.totalBidRemainQty = totalBidRemainQty; }
    public void setVolumeTurnoverRate(String volumeTurnoverRate) { this.volumeTurnoverRate = volumeTurnoverRate; }
    public void setPrevSameTimeVolume(String prevSameTimeVolume) { this.prevSameTimeVolume = prevSameTimeVolume; }
    public void setPrevSameTimeRate(String prevSameTimeRate) { this.prevSameTimeRate = prevSameTimeRate; }
    public void setHourCode(String hourCode) { this.hourCode = hourCode; }
    public void setMarketCloseCode(String marketCloseCode) { this.marketCloseCode = marketCloseCode; }
    public void setViStandardPrice(String viStandardPrice) { this.viStandardPrice = viStandardPrice; }
}