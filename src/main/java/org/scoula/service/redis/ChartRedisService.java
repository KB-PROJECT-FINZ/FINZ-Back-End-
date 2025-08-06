//package org.scoula.service.redis;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.scoula.domain.redis.dto.ChartResponse;
//import org.scoula.domain.redis.dto.Output2;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class ChartRedisService {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final ObjectMapper objectMapper;
//
//    private String formatTime(String raw) {
//        // "090000" → "09:00"
//        if (raw == null || raw.length() != 6) return raw;
//        return raw.substring(0, 2) + ":" + raw.substring(2, 4);
//    }
//    public String generateRedisKey(String stockCode) {
//        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        return "price:" + stockCode + ":" + today;
//    }
//
//    public boolean existsInRedis(String stockCode) {
//        String key = generateRedisKey(stockCode); // 예: price:005930:2025-08-05
//        return redisTemplate.hasKey(key);
//    }
//
//    public void saveToRedis(String stockCode, ChartResponse response) {
//
//        // output2 저장 (분봉 데이터만 저장)
//        List<Output2> minuteData = response.getOutput2();
//        if (minuteData == null || minuteData.isEmpty()) return;
//
//        String date = minuteData.get(0).getStck_bsop_date(); // 모든 분봉은 같은 날짜임
//        String key = "price:" + stockCode + ":" + date;
//
//        for (Output2 data : minuteData) {
//            String time = formatTime(data.getStck_cntg_hour());
//
//            // 이미 저장된 시간은 건너뜀
//            Boolean fieldExists = redisTemplate.opsForHash().hasKey(key, time);
//            if (Boolean.TRUE.equals(fieldExists)) continue;
//
//            Map<String, String> trimmed = Map.of(
//                    "stck_prpr", data.getStck_prpr(),
//                    "stck_oprc", data.getStck_oprc(),
//                    "stck_hgpr", data.getStck_hgpr(),
//                    "stck_lwpr", data.getStck_lwpr()
//            );
//
//            redisTemplate.opsForHash().put(key, time, trimmed);
//        }
//    }
//}
