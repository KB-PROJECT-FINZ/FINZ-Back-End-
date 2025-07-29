package org.scoula.util.chatbot;

import org.scoula.domain.chatbot.dto.RecommendationStock;

import java.util.Map;

// api 응답 => dto 바꾸는 매퍼
public class ProfileStockMapper {

    public static RecommendationStock fromMap(Map<String, Object> map) {
        return RecommendationStock.builder()
                .name((String) map.get("name"))
                .code((String) map.get("code"))
                .price(toNullableString(map.get("currentPrice")))
                .turnRate(toNullableString(map.get("turnoverRate")))
                .per(toNullableString(map.get("per")))
                .pbr(toNullableString(map.get("pbr")))
                .eps(toNullableString(map.get("eps")))
                .volume(toNullableString(map.get("volume")))
                .avgPrice(toNullableString(map.get("avgPrice")))
                .foreignRate(toNullableString(map.get("foreignRate")))
                .high(toNullableString(map.get("high")))
                .low(toNullableString(map.get("low")))
                .open(toNullableString(map.get("open")))
                .high52w(toNullableString(map.get("high52w")))
                .low52w(toNullableString(map.get("low52w")))
                .build();
    }

    private static String toNullableString(Object value) {
        return (value == null || value.toString().isBlank()) ? null : value.toString();
    }
}
