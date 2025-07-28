package org.scoula.controller.mocktrading;

import lombok.extern.log4j.Log4j2;
import org.scoula.service.mocktrading.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
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
        log.info("=== 시장 지수 조회 요청 ===");

        try {
            Map<String, Object> indices = marketService.getMarketIndices();
            log.info("시장 지수 조회 성공: {}", indices);
            return ResponseEntity.ok(indices);

        } catch (Exception e) {
            log.error("시장 지수 조회 실패", e);

            // 에러 시 더미 데이터 반환
            Map<String, Object> fallbackData = createFallbackIndices();
            return ResponseEntity.ok(fallbackData);
        }
    }

    /**
     * 거래량 순위 조회
     */
    @GetMapping("/ranking/volume")
    public ResponseEntity<List<Map<String, Object>>> getVolumeRanking(
            @RequestParam(defaultValue = "20") int limit) {

        log.info("=== 거래량 순위 조회 요청 (limit: {}) ===", limit);

        try {
            List<Map<String, Object>> ranking = marketService.getVolumeRanking(limit);
            log.info("거래량 순위 조회 성공: {} 건", ranking.size());
            return ResponseEntity.ok(ranking);

        } catch (Exception e) {
            log.error("거래량 순위 조회 실패", e);

            // 에러 시 더미 데이터 반환
            List<Map<String, Object>> fallbackData = createFallbackVolumeRanking(limit);
            return ResponseEntity.ok(fallbackData);
        }
    }

    /**
     * 시장 전체 현황 조회 (통합)
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getMarketOverview() {
        log.info("=== 시장 전체 현황 조회 요청 ===");

        try {
            Map<String, Object> overview = marketService.getMarketOverview();
            log.info("시장 현황 조회 성공");
            return ResponseEntity.ok(overview);

        } catch (Exception e) {
            log.error("시장 현황 조회 실패", e);

            // 에러 시 기본 응답
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "시장 데이터를 불러올 수 없습니다");
            errorResponse.put("indices", createFallbackIndices());
            errorResponse.put("topVolume", createFallbackVolumeRanking(10));

            return ResponseEntity.ok(errorResponse);
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

        log.info("=== 종목 검색 요청: '{}', limit: {} ===", query, limit);

        try {
            List<Map<String, Object>> stocks = searchStocksFromDB(query, limit);
            log.info("종목 검색 성공: {} 건", stocks.size());
            return ResponseEntity.ok(stocks);

        } catch (Exception e) {
            log.error("종목 검색 실패", e);
            return ResponseEntity.ok(new java.util.ArrayList<>());
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
                "  WHEN name LIKE ? THEN 3 " +     // 종목명으로 시작
                "  WHEN code LIKE ? THEN 4 " +     // 종목코드로 시작
                "  ELSE 5 " +                      // 나머지
                "END, name " +
                "LIMIT ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String exactQuery = query.trim();
            String startQuery = query.trim() + "%";

            stmt.setString(1, searchQuery);    // name LIKE
            stmt.setString(2, searchQuery);    // code LIKE
            stmt.setString(3, exactQuery);     // name = (정확히 일치)
            stmt.setString(4, exactQuery);     // code = (정확히 일치)
            stmt.setString(5, startQuery);     // name LIKE (시작하는)
            stmt.setString(6, startQuery);     // code LIKE (시작하는)
            stmt.setInt(7, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> stock = new HashMap<>();
                stock.put("code", rs.getString("code"));
                stock.put("name", rs.getString("name"));
                stock.put("imageUrl", rs.getString("image_url"));
                results.add(stock);
            }

            log.debug("종목 검색 결과: 검색어='{}', 결과={} 건", query, results.size());

        } catch (Exception e) {
            log.error("종목 검색 DB 오류: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 에러 시 사용할 더미 시장 지수 데이터
     */
    private Map<String, Object> createFallbackIndices() {
        Map<String, Object> fallback = new HashMap<>();

        Map<String, Object> kospi = new HashMap<>();
        kospi.put("name", "KOSPI");
        kospi.put("value", 2634.15);
        kospi.put("change", 15.23);
        kospi.put("changePercent", 0.58);
        kospi.put("isPositive", true);

        Map<String, Object> kosdaq = new HashMap<>();
        kosdaq.put("name", "KOSDAQ");
        kosdaq.put("value", 851.47);
        kosdaq.put("change", -8.32);
        kosdaq.put("changePercent", -0.97);
        kosdaq.put("isPositive", false);

        fallback.put("kospi", kospi);
        fallback.put("kosdaq", kosdaq);

        return fallback;
    }

    /**
     * 에러 시 사용할 더미 거래량 순위 데이터 (이미지 URL 포함)
     */
    private List<Map<String, Object>> createFallbackVolumeRanking(int limit) {
        List<Map<String, Object>> fallback = new java.util.ArrayList<>();

        String[] stockNames = {"삼성전자", "SK하이닉스", "NAVER", "현대차", "LG화학",
                "삼성SDI", "카카오", "삼성바이오로직스", "셀트리온", "카카오뱅크"};
        String[] stockCodes = {"005930", "000660", "035420", "005380", "051910",
                "006400", "035720", "207940", "068270", "323410"};

        for (int i = 0; i < Math.min(limit, stockNames.length); i++) {
            Map<String, Object> stock = new HashMap<>();
            stock.put("code", stockCodes[i]);
            stock.put("name", stockNames[i]);
            stock.put("currentPrice", (int)(Math.random() * 100000) + 10000);
            stock.put("change", (int)(Math.random() * 5000) - 2500);
            stock.put("changePercent", (Math.random() * 10) - 5);
            stock.put("isPositive", Math.random() > 0.5);
            stock.put("tradingVolume", (long)(Math.random() * 5000000000L) + 1000000000L);
            stock.put("volume", (long)(Math.random() * 1000000) + 100000);
            stock.put("rank", i + 1);

            // DB에서 이미지 URL 조회 시도
            String imageUrl = getStockImageUrl(stockCodes[i]);
            stock.put("imageUrl", imageUrl);

            fallback.add(stock);
        }

        return fallback;
    }

    /**
     * DB에서 종목 코드로 이미지 URL 조회 (fallback용)
     */
    private String getStockImageUrl(String stockCode) {
        String imageUrl = null;
        String sql = "SELECT image_url FROM stocks WHERE code = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stockCode);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                imageUrl = rs.getString("image_url");
                log.debug("이미지 URL 조회 성공: {} -> {}", stockCode, imageUrl);
            } else {
                log.debug("이미지 URL 없음: {}", stockCode);
            }

        } catch (Exception e) {
            log.warn("이미지 URL 조회 실패: {} - {}", stockCode, e.getMessage());
        }

        return imageUrl;
    }
}