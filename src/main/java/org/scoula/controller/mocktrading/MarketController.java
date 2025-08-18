package org.scoula.controller.mocktrading;

import lombok.extern.log4j.Log4j2;
import org.scoula.service.mocktrading.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@Log4j2
public class MarketController {

    @Autowired
    private MarketService marketService;

    @Autowired
    private DataSource dataSource;

    /**
     * 시장 지수 조회 (코스피/코스닥)
     */
    @GetMapping("/indices")
    public ResponseEntity<Map<String, Object>> getMarketIndices() {
        try {
            Map<String, Object> indices = marketService.getMarketIndices();
            return ResponseEntity.ok(indices);

        } catch (Exception e) {
            log.error("시장 지수 조회 실패", e);

            // 에러 시 더미 데이터 반환
            Map<String, Object> fallbackData = createFallbackIndices();
            return ResponseEntity.ok(fallbackData);
        }
    }

    /**
     * 거래량 순위 조회 엔드포인트 - 탭 기능 지원
     * GET /api/market/ranking/volume?limit=20&blngClsCode=3
     */
    @GetMapping("/ranking/volume")
    public ResponseEntity<?> getVolumeRanking(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "3") String blngClsCode) {

        try {

            if (limit < 1 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "limit은 1~100 사이의 값이어야 합니다", "limit", limit));
            }

            if (!isValidBlngClsCode(blngClsCode)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "유효하지 않은 blngClsCode입니다", "blngClsCode", blngClsCode));
            }

            List<Map<String, Object>> ranking = marketService.getVolumeRanking(limit, blngClsCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ranking);
            response.put("limit", limit);
            response.put("rankingType", blngClsCode);
            response.put("description", getTabDescription(blngClsCode));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ranking); // 기존 호환성을 위해 직접 리스트 반환

        } catch (Exception e) {
            log.error("❌ 거래량 순위 조회 실패: {}", e.getMessage(), e);

            // 에러 시 더미 데이터 반환 (기존 동작 유지)
            List<Map<String, Object>> fallbackData = createFallbackVolumeRanking(limit);
            return ResponseEntity.ok(fallbackData);
        }
    }

    /**
     * 시장 전체 현황 조회 (통합) - 탭 기능 지원
     * GET /api/market/overview?blngClsCode=3
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getMarketOverview(
            @RequestParam(defaultValue = "3") String blngClsCode) {

        try {
            if (!isValidBlngClsCode(blngClsCode)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "유효하지 않은 blngClsCode입니다");
                errorResponse.put("blngClsCode", blngClsCode);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Map<String, Object> overview = marketService.getMarketOverview(blngClsCode);
            log.info("시장 현황 조회 성공");
            return ResponseEntity.ok(overview);

        } catch (Exception e) {
            log.error("시장 현황 조회 실패", e);

            // 에러 시 기본 응답 (기존 동작 유지)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "시장 데이터를 불러올 수 없습니다");
            errorResponse.put("indices", createFallbackIndices());
            errorResponse.put("topVolume", createFallbackVolumeRanking(10));
            errorResponse.put("rankingType", blngClsCode);

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 통합 거래량 순위 조회 엔드포인트 - 탭 기능 지원
     * GET /api/market/ranking/combined?limit=20&blngClsCode=3
     */
    @GetMapping("/ranking/combined")
    public ResponseEntity<?> getCombinedVolumeRanking(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "3") String blngClsCode) {

        try {
            if (limit < 1 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "limit은 1~100 사이의 값이어야 합니다"));
            }

            if (!isValidBlngClsCode(blngClsCode)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "유효하지 않은 blngClsCode입니다"));
            }

            Map<String, Object> result = marketService.getCombinedVolumeRanking(limit, blngClsCode);

            log.info("✅ 통합 거래량 순위 조회 성공");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ 통합 거래량 순위 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("limit", limit);
            errorResponse.put("rankingType", blngClsCode);
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 통합 거래량 순위 (단일 리스트) 조회 엔드포인트 - 탭 기능 지원
     * GET /api/market/ranking/unified?limit=20&blngClsCode=3
     */
    @GetMapping("/ranking/unified")
    public ResponseEntity<?> getUnifiedVolumeRanking(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "3") String blngClsCode) {

        try {
            if (limit < 1 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "limit은 1~100 사이의 값이어야 합니다"));
            }

            if (!isValidBlngClsCode(blngClsCode)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "유효하지 않은 blngClsCode입니다"));
            }

            List<Map<String, Object>> ranking = marketService.getUnifiedVolumeRanking(limit, blngClsCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ranking);
            response.put("limit", limit);
            response.put("rankingType", blngClsCode);
            response.put("description", getTabDescription(blngClsCode));
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ 통합 거래량 순위(단일) 조회 성공 - {} 건 반환", ranking.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 통합 거래량 순위(단일) 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("limit", limit);
            errorResponse.put("rankingType", blngClsCode);
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API 상태 확인용 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "Mock Trading API");

        return ResponseEntity.ok(health);
    }

    /**
     * 종목 검색 API
     */
    @GetMapping("/stocks/search")
    public ResponseEntity<List<Map<String, Object>>> searchStocks(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            List<Map<String, Object>> stocks = searchStocksFromDB(query, limit);
            return ResponseEntity.ok(stocks);

        } catch (Exception e) {
            log.error("종목 검색 실패", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * blngClsCode 유효성 검증
     */
    private boolean isValidBlngClsCode(String blngClsCode) {
        return blngClsCode != null &&
                (blngClsCode.equals("0") || blngClsCode.equals("3"));
    }

    /**
     * 탭 코드에 따른 설명 반환
     */
    private String getTabDescription(String blngClsCode) {
        switch (blngClsCode) {
            case "0": return "평균거래량";
            case "3": return "거래금액순";
            default: return "평균거래량";
        }
    }

    /**
     * DB에서 종목 검색
     */
    private List<Map<String, Object>> searchStocksFromDB(String query, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String searchQuery = "%" + query.trim() + "%";
        String sql = "SELECT code, name, image_url FROM stocks " +
                "WHERE (name LIKE ? OR code LIKE ?) " +
                "ORDER BY " +
                "CASE " +
                "  WHEN name = ? THEN 1 " +        // 정확히 일치하는 종목명
                "  WHEN code = ? THEN 2 " +        // 정확히 일치하는 종목코드
                "  WHEN name LIKE ? THEN 3 " +     // 종목명이 검색어로 시작
                "  WHEN code LIKE ? THEN 4 " +     // 종목코드가 검색어로 시작
                "  ELSE 5 " +                      // 나머지
                "END, name ASC " +
                "LIMIT ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String trimmedQuery = query.trim();
            String startsWithQuery = trimmedQuery + "%";

            stmt.setString(1, searchQuery);        // name LIKE %query%
            stmt.setString(2, searchQuery);        // code LIKE %query%
            stmt.setString(3, trimmedQuery);       // name = query
            stmt.setString(4, trimmedQuery);       // code = query
            stmt.setString(5, startsWithQuery);    // name LIKE query%
            stmt.setString(6, startsWithQuery);    // code LIKE query%
            stmt.setInt(7, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> stock = new HashMap<>();
                stock.put("code", rs.getString("code"));
                stock.put("name", rs.getString("name"));
                stock.put("imageUrl", rs.getString("image_url"));
                results.add(stock);
            }

        } catch (Exception e) {
            log.error("DB 종목 검색 실패: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * 더미 시장 지수 데이터 생성
     */
    private Map<String, Object> createFallbackIndices() {
        Map<String, Object> indices = new HashMap<>();

        Map<String, Object> kospi = new HashMap<>();
        kospi.put("name", "코스피");
        kospi.put("value", 2580.45);
        kospi.put("change", 15.25);
        kospi.put("changePercent", 0.59);
        kospi.put("isPositive", true);

        Map<String, Object> kosdaq = new HashMap<>();
        kosdaq.put("name", "코스닥");
        kosdaq.put("value", 850.12);
        kosdaq.put("change", -3.88);
        kosdaq.put("changePercent", -0.45);
        kosdaq.put("isPositive", false);

        indices.put("kospi", kospi);
        indices.put("kosdaq", kosdaq);

        return indices;
    }

    /**
     * 더미 거래량 순위 데이터 생성
     */
    private List<Map<String, Object>> createFallbackVolumeRanking(int limit) {
        List<Map<String, Object>> ranking = new ArrayList<>();

        String[] stockNames = {"삼성전자", "SK하이닉스", "NAVER", "현대차", "LG화학", "삼성SDI", "카카오", "삼성바이오로직스", "셀트리온", "카카오뱅크"};
        String[] stockCodes = {"005930", "000660", "035420", "005380", "051910", "006400", "035720", "207940", "068270", "323410"};

        for (int i = 0; i < Math.min(limit, stockNames.length); i++) {
            Map<String, Object> stock = new HashMap<>();
            stock.put("code", stockCodes[i]);
            stock.put("name", stockNames[i]);
            stock.put("currentPrice", 50000 + (int)(Math.random() * 100000));
            stock.put("change", (int)(Math.random() * 5000) - 2500);
            stock.put("changePercent", Math.random() * 10 - 5);
            stock.put("isPositive", Math.random() > 0.5);
            stock.put("tradingVolume", (long)(Math.random() * 5000000000L) + 1000000000L);
            stock.put("rank", i + 1);
            stock.put("imageUrl", "https://file.alphasquare.co.kr/media/images/stock_logo/kr/" + stockCodes[i] + ".png");
            ranking.add(stock);
        }

        return ranking;
    }
}