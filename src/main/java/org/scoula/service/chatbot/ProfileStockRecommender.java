package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.api.mocktrading.PriceApi;
import org.scoula.domain.chatbot.dto.ChatPriceResponse;
import org.scoula.domain.chatbot.dto.RecommendationStock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class ProfileStockRecommender {

    private final ObjectMapper objectMapper;

    public List<RecommendationStock> getRecommendedStocksByProfile(List<String> tickers, List<String> names) {

        log.info("✅ tickers.size = {}, names.size = {}", tickers.size(), names.size());
        log.info("✅ tickers: {}", tickers);
        log.info("✅ names: {}", names);
        List<RecommendationStock> stocks = new ArrayList<>();



        for (int i = 0; i < tickers.size(); i++) {
            String ticker = tickers.get(i);
            String name = names.get(i);

            try {
                log.info("🔎 종목 상세 조회 요청 → {} ({})", name, ticker);

                var raw = PriceApi.getPriceData(ticker);
                log.info("🧾 -----------price raw response for {}: {}", ticker, raw.toPrettyString());


                ChatPriceResponse response = objectMapper.treeToValue(
                        PriceApi.getPriceData(ticker), ChatPriceResponse.class
                );

                RecommendationStock stock = toRecommendationStock(name, ticker,response.getOutput());
                stocks.add(stock);
                log.info("✅ 상세 데이터 변환 완료 → {} ({})", name, ticker);


            } catch (Exception e) {
                log.warn("❌ 상세 종목 조회 실패 - {}({}): {}", name, ticker, e.getMessage());
            }
        }

        log.info("📊 최종 상세 종목 수: {} / {}", stocks.size(), tickers.size());
        return stocks;

    }

    private RecommendationStock toRecommendationStock(String name, String code, ChatPriceResponse.Output o) {
        return RecommendationStock.builder()
                .name(name)
                .code(code)
                .price(parseDouble(o.getStck_prpr()))
                .per(parseDouble(o.getPer()))
                .eps(parseDouble(o.getEps()))
                .pbr(parseDouble(o.getPbr()))
                .open(parseDouble(o.getStck_oprc()))
                .high(parseDouble(o.getStck_hgpr()))
                .low(parseDouble(o.getStck_lwpr()))
                .volume(parseDouble(o.getAcml_vol()))
                .avgPrice(parseDouble(o.getWghn_avrg_stck_prc()))
                .foreignRate(parseDouble(o.getHts_frgn_ehrt()))
                .turnRate(parseDouble(o.getVol_tnrt()))
                .high52w(parseDouble(o.getD250_hgpr()))
                .low52w(parseDouble(o.getD250_lwpr()))
                .build();
    }

    private Double parseDouble(String str) {
        try {
            return (str == null || str.isBlank()) ? null : Double.parseDouble(str.replace(",", ""));
        } catch (NumberFormatException e) {
            log.warn("❗ 숫자 변환 실패: '{}'", str);
            return null;
        }
    }
}
