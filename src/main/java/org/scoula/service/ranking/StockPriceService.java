package org.scoula.service.ranking;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.scoula.api.mocktrading.PriceApi;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class StockPriceService {


    //여러 종목 현재가 반환
    public Map<String, BigDecimal> getCurrentPrices(List<String> stockCodes) {
        Map<String, BigDecimal> priceMap = new HashMap<>();

        for (String code : stockCodes) {
            try {
                JsonNode response = PriceApi.getPriceData(code);

                // 현재가 필드: output.stck_prpr (String 형식의 숫자)
                String priceStr = response.path("output").path("stck_prpr").asText();
                BigDecimal price = new BigDecimal(priceStr);

                priceMap.put(code, price);

            } catch (Exception e) {
                log.warn("❌ 종목 코드 {} 가격 조회 실패: {}", code, e.getMessage());
                priceMap.put(code, BigDecimal.ZERO);
            }
        }

        return priceMap;
    }
}

