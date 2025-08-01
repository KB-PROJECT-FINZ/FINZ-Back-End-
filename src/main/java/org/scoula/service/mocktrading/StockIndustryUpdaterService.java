package org.scoula.service.mocktrading;

import lombok.extern.log4j.Log4j2;
import org.scoula.api.mocktrading.BasicInfoService;
import org.scoula.mapper.StockMapper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class StockIndustryUpdaterService {

    private final StockMapper stockMapper;


    private final BasicInfoService basicInfoService;

    public StockIndustryUpdaterService(StockMapper stockMapper, BasicInfoService basicInfoService) {
        this.stockMapper = stockMapper;
        this.basicInfoService = basicInfoService;
    }

    public void updateAllStockIndustries() {
        List<Map<String, Object>> stocks = stockMapper.getAllStocks();
        log.info("🔍 총 {}개 종목 업종 업데이트 시작", stocks.size());

        for (Map<String, Object> stock : stocks) {
            String code = (String) stock.get("code");

            try {
                JsonNode info = basicInfoService.getBasicInfo(code).get("output");

                if (info == null || info.isEmpty()) {
                    log.warn("⚠️ [{}] 기본정보 응답 없음, 건너뜀", code);
                    continue;
                }

                Map<String, Object> param = new HashMap<>();
                param.put("code", code);
//                param.put("industry_lcls", info.get("idx_bztp_lcls_cd_name").asText(null));      // 대분류 (예: 제조업) <- api 문서 오류 개판이네
                param.put("industry_mcls", info.get("idx_bztp_mcls_cd_name").asText(null));      // 중분류 (예: 화학)
                param.put("industry_scls", info.get("idx_bztp_scls_cd_name").asText(null));      // 소분류
                param.put("industry_std_name", info.get("std_idst_clsf_cd_name").asText(null));  // 표준 산업 분류명
                param.put("stock_type_code", info.get("stck_kind_cd").asText(null));


                stockMapper.updateStockIndustry(param);

                log.info("[{}] 업종 업데이트 완료 → {}, {}, {}, {}, {}",
                        code,
                        param.get("industry_mcls"),
                        param.get("industry_scls"),
                        param.get("industry_std_name"),
                        param.get("stock_type_code"));

                Thread.sleep(200); // API rate limit 대응

            } catch (Exception e) {
                log.warn("❌ [{}] 업종 업데이트 실패: {}", code, e.getMessage());
            }
        }

        log.info("전체 업종 업데이트 완료!");
    }
}
