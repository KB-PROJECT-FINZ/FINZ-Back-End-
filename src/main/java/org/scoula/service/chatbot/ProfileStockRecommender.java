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

/**
 * ProfileStockRecommender
 *
 * <p>사용자 프로필(성향)에 맞춰 종목 상세 데이터를 보강해주는 서비스.</p>
 *
 * <h3>전체 흐름</h3>
 * <ol>
 *   <li>티커/종목명 리스트를 입력받음</li>
 *   <li>PriceApi 호출 → 종목별 상세 데이터(JSON) 조회</li>
 *   <li>응답(JSON → DTO 변환) 후 RecommendationStock으로 매핑</li>
 *   <li>EPS/BPS를 기반으로 ROE 계산 및 지표 보강</li>
 *   <li>최종 RecommendationStock 리스트 반환</li>
 * </ol>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>외부 PriceApi 응답을 내부 도메인 모델(RecommendationStock)로 표준화</li>
 *   <li>EPS, BPS를 활용해 추가 계산치(ROE) 제공</li>
 *   <li>실패 시 로그 남기고 건너뛰기 → 전체 흐름 중단 방지</li>
 *   <li>최종적으로 "성향 기반 추천"이나 "종목 분석"에 활용될 데이터셋 완성</li>
 * </ul>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ProfileStockRecommender {

    private final ObjectMapper objectMapper;

    /**
     * 주어진 종목 리스트에 대해 상세 지표 조회 및 변환 수행
     *
     * @param tickers 종목 코드 리스트
     * @param names   종목명 리스트
     * @return 상세 데이터가 채워진 RecommendationStock 리스트
     */
    public List<RecommendationStock> getRecommendedStocksByProfile(List<String> tickers, List<String> names) {


        log.info("✅ 종목 조회 시작 - 총 {}개", tickers.size());

        List<RecommendationStock> stocks = new ArrayList<>();



        for (int i = 0; i < tickers.size(); i++) {
            String ticker = tickers.get(i);
            String name = names.get(i);

            try {
                log.info("🔎 종목 상세 조회 요청 → {} ({})", name, ticker);

                var raw = PriceApi.getPriceData(ticker);

                log.info("🧾 price API 응답 수신 완료 → {} ({})", name, ticker);



                ChatPriceResponse response = objectMapper.treeToValue(raw, ChatPriceResponse.class);

                RecommendationStock stock = toRecommendationStock(name, ticker,response.getOutput());

                log.info("📈 {} EPS: {}, BPS: {}, ROE: {}, PER: {}, PBR: {}",
                        stock.getName(), stock.getEps(), stock.getBps(), stock.getRoe(), stock.getPer(), stock.getPbr());

                stocks.add(stock);
                log.info("✅ 상세 데이터 변환 완료 → {} ({})", name, ticker);




            } catch (Exception e) {
                log.warn("❌ 상세 종목 조회 실패 - {}({}): {}", name, ticker, e.getMessage());
            }
        }

        log.info("📊 최종 상세 종목 수: {} / {}", stocks.size(), tickers.size());
        return stocks;

    }

    /** Price API 응답을 RecommendationStock으로 변환 (EPS/BPS → ROE 추가 계산) */
    private RecommendationStock toRecommendationStock(String name, String code, ChatPriceResponse.Output o) {

        Double eps = parseDouble(o.getEps());
        Double bps = parseDouble(o.getBps());

        Double roe = (eps != null && bps != null && bps != 0.0) ? (eps / bps * 100.0) : null;
        log.info("🧪 {} EPS: {}, BPS: {}, ROE: {}", code, eps, bps, roe);

        return RecommendationStock.builder()
                .name(name)
                .code(code)
                .price(parseDouble(o.getStck_prpr()))
                .per(parseDouble(o.getPer()))
                .eps(parseDouble(o.getEps()))
                .pbr(parseDouble(o.getPbr()))
                .roe(roe)
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

    /** 문자열 → Double 변환 (숫자/콤마 처리, 실패 시 null 반환) */
    private Double parseDouble(String str) {
        try {
            return (str == null || str.isBlank()) ? null : Double.parseDouble(str.replace(",", ""));
        } catch (NumberFormatException e) {
            log.warn("❗ 숫자 변환 실패: '{}'", str);
            return null;
        }
    }
}
