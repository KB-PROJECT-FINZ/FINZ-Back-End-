package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.scoula.api.mocktrading.PriceApi;
import org.scoula.domain.chatbot.dto.ChatPriceResponse;
import org.scoula.domain.chatbot.dto.RecommendationStock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
public class ProfileStockRecommender {

    private final ObjectMapper objectMapper;

    public List<RecommendationStock> getRecommendedStocksByProfile(List<String> tickers, List<String> names) {
        List<RecommendationStock> stocks = new ArrayList<>();

        for (int i = 0; i < tickers.size(); i++) {
            String ticker = tickers.get(i);
            String name = names.get(i);

            try {
                ChatPriceResponse response = objectMapper.treeToValue(
                        PriceApi.getPriceData(ticker), ChatPriceResponse.class
                );

                RecommendationStock stock = toRecommendationStock(name, ticker,response.getOutput());
                stocks.add(stock);

            } catch (Exception e) {
                System.err.println("❌ " + ticker + " 데이터 조회 실패: " + e.getMessage());
            }
        }

        return stocks;
    }

    private RecommendationStock toRecommendationStock(String name,String code, ChatPriceResponse.Output o) {
        return RecommendationStock.builder()
                .name(name)
                .code(code)
                .price(o.getStck_prpr())
                .per(o.getPer())
                .eps(o.getEps())
                .pbr(o.getPbr())
                .open(o.getStck_oprc())
                .high(o.getStck_hgpr())
                .low(o.getStck_lwpr())
                .volume(o.getAcml_vol())
                .avgPrice(o.getWghn_avrg_stck_prc())
                .foreignRate(o.getHts_frgn_ehrt())
                .turnRate(o.getVol_tnrt())
                .high52w(o.getD250_hgpr())
                .low52w(o.getD250_lwpr())
                .build();
    }
}
