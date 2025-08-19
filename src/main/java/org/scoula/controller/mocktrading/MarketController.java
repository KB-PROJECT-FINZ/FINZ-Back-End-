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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
}