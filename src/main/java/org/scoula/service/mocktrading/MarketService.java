package org.scoula.service.mocktrading;

import lombok.extern.log4j.Log4j2;
import org.scoula.api.mocktrading.MarketIndexApi;
import org.scoula.api.mocktrading.VolumeRankingApi;
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
     * 시장 지수 조회 (코스피/코스닥) - 기존 메서드 유지
     */
    public Map<String, Object> getMarketIndices() throws Exception {

        try {
            if (marketIndexApi == null) {
                throw new Exception("MarketIndexApi가 주입되지 않았습니다");
            }
            Map<String, Object> result = marketIndexApi.getAllMarketIndices();
            return result;
        } catch (Exception e) {
            log.error("❌ MarketService.getMarketIndices() 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 거래량 순위 조회 - 탭 기능 지원
     * @param limit 조회할 종목 수
     * @param blngClsCode 소속 구분 코드 (0:평균거래량 3:거래금액순)
     */
    public List<Map<String, Object>> getVolumeRanking(int limit, String blngClsCode) throws Exception {

        try {
            if (volumeRankingApi == null) {
                throw new Exception("VolumeRankingApi가 주입되지 않았습니다");
            }

            // 탭별 설명 로깅
            String tabDescription = getTabDescription(blngClsCode);

            List<Map<String, Object>> result = volumeRankingApi.getVolumeRanking("J", limit, blngClsCode);
            log.info("{} 순위 조회 성공: {} 건", tabDescription, result != null ? result.size() : 0);

            return result;
        } catch (Exception e) {
            log.error("❌ MarketService.getVolumeRanking() 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 시장 전체 현황 조회 (통합) - 탭 기능 지원
     */
    public Map<String, Object> getMarketOverview(String blngClsCode) throws Exception {
        Map<String, Object> overview = new HashMap<>();

        try {
            String tabDescription = getTabDescription(blngClsCode);
            log.info("🔍 시장 전체 현황 조회 - {}", tabDescription);

            // 시장 지수와 거래량 순위를 동시에 조회
            Map<String, Object> indices = getMarketIndices();
            List<Map<String, Object>> topVolume = getVolumeRanking(10, blngClsCode);

            overview.put("indices", indices);
            overview.put("topVolume", topVolume);
            overview.put("success", true);
            overview.put("updateTime", System.currentTimeMillis());
            overview.put("rankingType", blngClsCode);
            overview.put("description", tabDescription);

            log.info("✅ 시장 전체 현황 조회 성공 - {}", tabDescription);

        } catch (Exception e) {
            // 에러 발생 시에도 기본 구조 유지
            overview.put("indices", null);
            overview.put("topVolume", null);
            overview.put("success", false);
            overview.put("error", e.getMessage());
            overview.put("updateTime", System.currentTimeMillis());
            overview.put("rankingType", blngClsCode);

            log.error("❌ 시장 전체 현황 조회 실패: {}", e.getMessage(), e);
            // 예외를 다시 던져서 컨트롤러에서 처리
            throw e;
        }

        return overview;
    }
    /**
     * 기존 호환성을 위한 오버로드 메서드
     */
    public Map<String, Object> getMarketOverview() throws Exception {
        return getMarketOverview("3");
    }


    /**
     * 코스피와 코스닥 거래량 순위를 통합 조회 - 탭 기능 지원
     */
    public Map<String, Object> getCombinedVolumeRanking(int limit, String blngClsCode) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try {
            String tabDescription = getTabDescription(blngClsCode);
            log.info("🔍 통합 {} 순위 조회 시작", tabDescription);

            // 코스피와 코스닥 거래량 순위를 각각 조회
            List<Map<String, Object>> kospiRanking = volumeRankingApi.getVolumeRanking("J", limit, blngClsCode);
            List<Map<String, Object>> kosdaqRanking = volumeRankingApi.getVolumeRanking("Q", limit, blngClsCode);

            result.put("kospi", kospiRanking);
            result.put("kosdaq", kosdaqRanking);
            result.put("success", true);
            result.put("rankingType", blngClsCode);
            result.put("description", tabDescription);

            log.info("✅ 통합 {} 순위 조회 성공 - 코스피: {}건, 코스닥: {}건",
                    tabDescription,
                    kospiRanking != null ? kospiRanking.size() : 0,
                    kosdaqRanking != null ? kosdaqRanking.size() : 0);

        } catch (Exception e) {
            result.put("kospi", null);
            result.put("kosdaq", null);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("rankingType", blngClsCode);

            log.error("❌ 통합 거래량 순위 조회 실패: {}", e.getMessage(), e);
            throw e;
        }

        return result;
    }


    /**
     * 통합 거래량 순위 (코스피+코스닥 합쳐서) - 탭 기능 지원
     */
    public List<Map<String, Object>> getUnifiedVolumeRanking(int limit, String blngClsCode) throws Exception {
        log.info("🔍 통합 거래량 순위 조회 - limit: {}, type: {}", limit, getTabDescription(blngClsCode));
        return volumeRankingApi.getCombinedVolumeRanking(limit, blngClsCode);
    }

    /**
     * 탭 코드에 따른 설명 반환
     */
    private String getTabDescription(String blngClsCode) {
        switch (blngClsCode) {
            case "0": return "평균거래량";
            case "1": return "거래증가율";
            case "2": return "평균거래회전율";
            case "3": return "거래금액순";
            case "4": return "평균거래금액회전율";
            default: return "거래대금순위";
        }
    }
}