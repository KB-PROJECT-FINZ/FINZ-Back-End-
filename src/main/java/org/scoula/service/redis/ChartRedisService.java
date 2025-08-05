package org.scoula.service.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.scoula.domain.redis.dto.ChartResponse;
import org.scoula.domain.redis.dto.Output2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChartRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper; // jackson 사용
    private String formatTime(String raw) {
        // "090000" → "09:00"
        if (raw == null || raw.length() != 6) return raw;
        return raw.substring(0, 2) + ":" + raw.substring(2, 4);
    }

    public void saveToRedis(String stockCode, ChartResponse response) {
        // output1 저장
        Map<String, String> output1Map = objectMapper.convertValue(response.getOutput1(), new TypeReference<>() {});
        redisTemplate.opsForHash().putAll("price:" + stockCode + ":latest", output1Map);

        // output2 저장
        List<Output2> minuteData = response.getOutput2();
        if (minuteData.isEmpty()) return;

        String date = minuteData.get(0).getStck_bsop_date(); // 모든 분봉은 같은 날짜임
        String key = "price:" + stockCode + ":" + date;

        for (Output2 data : minuteData) {
            String time = formatTime(data.getStck_cntg_hour());
            //필드 존재 여부 확인
            Boolean fieldExists = redisTemplate.opsForHash().hasKey(key, time);
            if (Boolean.TRUE.equals(fieldExists)) continue;

            Map<String, String> trimmed = Map.of(
                    "stck_prpr", data.getStck_prpr(),
                    "stck_oprc", data.getStck_oprc(),
                    "stck_hgpr", data.getStck_hgpr(),
                    "stck_lwpr", data.getStck_lwpr()
            );

            redisTemplate.opsForHash().put(key, time, trimmed);
        }
    }
}
