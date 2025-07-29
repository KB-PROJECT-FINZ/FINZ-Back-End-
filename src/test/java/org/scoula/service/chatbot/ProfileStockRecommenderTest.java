package org.scoula.service.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.scoula.domain.chatbot.dto.RecommendationStock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileStockRecommenderTest {

    @Test
    void getRecommendedStocksByProfile() {
        // ObjectMapper 주입
        ProfileStockRecommender recommender = new ProfileStockRecommender(new ObjectMapper());

        // 테스트용 티커/이름
        List<String> tickers = List.of("005930"); // 삼성전자
        List<String> names = List.of("삼성전자");

        List<RecommendationStock> results = recommender.getRecommendedStocksByProfile(tickers, names);

        // 검증
        assertNotNull(results, "결과는 null이면 안 됨");
        assertFalse(results.isEmpty(), "결과가 비어있으면 안 됨");

        RecommendationStock stock = results.get(0);
        System.out.println("📈 주식 데이터: " + stock);

        // 세부 필드 확인
        assertEquals("삼성전자", stock.getName());
        assertNotNull(stock.getPrice(), "현재가(price)는 null이면 안 됨");
    }
}