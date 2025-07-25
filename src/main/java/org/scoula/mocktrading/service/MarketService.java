package org.scoula.mocktrading.service;

import lombok.extern.log4j.Log4j2;
import org.scoula.mocktrading.external.MarketIndexApi;
import org.scoula.mocktrading.external.VolumeRankingApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class MarketService {

    @Autowired
    private MarketIndexApi marketIndexApi;

    @Autowired
    private VolumeRankingApi volumeRankingApi;

    /**
     * 시장 지수 조회 (코스피/코스닥)
     */
    public Map<String, Object> getMarketIndices() throws Exception {
        log.info("🔍 MarketService.getMarketIndices() 시작");

        try {
            if (marketIndexApi == null) {
                throw new Exception("MarketIndexApi가 주입되지 않았습니다");
            }

            log.info("📞 MarketIndexApi.getAllMarketIndices() 호출");
            Map<String, Object> result = marketIndexApi.getAllMarketIndices();
            log.info("✅ 시장 지수 조회 성공: {}", result);

            return result;
        } catch (Exception e) {
            log.error("❌ MarketService.getMarketIndices() 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 거래량 순위 조회
     */
    public List<Map<String, Object>> getVolumeRanking(int limit) throws Exception {
        log.info("🔍 MarketService.getVolumeRanking() 시작 - limit: {}", limit);

        try {
            if (volumeRankingApi == null) {
                throw new Exception("VolumeRankingApi가 주입되지 않았습니다");
            }

            log.info("📞 VolumeRankingApi.getVolumeRanking('J', {}) 호출", limit);
            List<Map<String, Object>> result = volumeRankingApi.getVolumeRanking("J", limit);
            log.info("✅ 거래량 순위 조회 성공: {} 건", result != null ? result.size() : 0);

            return result;
        } catch (Exception e) {
            log.error("❌ MarketService.getVolumeRanking() 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 시장 전체 현황 조회 (통합)
     */
    public Map<String, Object> getMarketOverview() throws Exception {
        Map<String, Object> overview = new HashMap<>();

        try {
            // 시장 지수와 거래량 순위를 동시에 조회
            Map<String, Object> indices = getMarketIndices();
            List<Map<String, Object>> topVolume = getVolumeRanking(10);

            overview.put("indices", indices);
            overview.put("topVolume", topVolume);
            overview.put("success", true);
            overview.put("updateTime", System.currentTimeMillis());

        } catch (Exception e) {
            // 에러 발생 시에도 기본 구조 유지
            overview.put("indices", null);
            overview.put("topVolume", null);
            overview.put("success", false);
            overview.put("error", e.getMessage());
            overview.put("updateTime", System.currentTimeMillis());

            // 예외를 다시 던져서 컨트롤러에서 처리
            throw e;
        }

        return overview;
    }

    /**
     * 코스피와 코스닥 거래량 순위를 통합 조회
     */
    public Map<String, Object> getCombinedVolumeRanking(int limit) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try {
            // 코스피와 코스닥 거래량 순위를 각각 조회
            List<Map<String, Object>> kospiRanking = volumeRankingApi.getVolumeRanking("J", limit);
            List<Map<String, Object>> kosdaqRanking = volumeRankingApi.getVolumeRanking("Q", limit);

            result.put("kospi", kospiRanking);
            result.put("kosdaq", kosdaqRanking);
            result.put("success", true);

        } catch (Exception e) {
            result.put("kospi", null);
            result.put("kosdaq", null);
            result.put("success", false);
            result.put("error", e.getMessage());

            throw e;
        }

        return result;
    }

    /**
     * 통합 거래량 순위 (코스피+코스닥 합쳐서)
     */
    public List<Map<String, Object>> getUnifiedVolumeRanking(int limit) throws Exception {
        return volumeRankingApi.getCombinedVolumeRanking(limit);
    }
}