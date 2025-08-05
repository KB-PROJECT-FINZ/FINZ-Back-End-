package org.scoula.service.redis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.api.mocktrading.MinuteChartApi;
import org.scoula.domain.redis.dto.ChartResponse;
import org.scoula.service.mocktrading.MarketService;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupScheduler {

    private final MarketService marketService;
    private final MinuteChartApi minuteChartApi;
    private final ChartRedisService chartRedisService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void preloadTopStocks() {

        log.info("🔥 StartupScheduler 실행됨");
        try {
            List<Map<String, Object>> topStocks = marketService.getVolumeRanking(20, "3");
//            for (Map<String, Object> stock : topStocks) {
//                System.out.println("stock map: " + stock);
//            }
            for (Map<String, Object> stock : topStocks) {
                String stockCode = (String) stock.get("code");

                JsonNode chartData = minuteChartApi.getFullDayMinuteChartData(stockCode);
                ChartResponse response = objectMapper.treeToValue(chartData, ChartResponse.class);

                chartRedisService.saveToRedis(stockCode, response);
                log.info("Redis에 저장 완료 - {}", stockCode);

                //Thread.sleep(200);
            }

        } catch (Exception e) {
            log.error("Redis 초기화 중 오류", e);
        }
    }
}
