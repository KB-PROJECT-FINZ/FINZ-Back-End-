package org.scoula.util.chatbot;

import org.scoula.domain.chatbot.dto.RecommendationStock;

import java.util.Map;

// api 응답 => dto 바꾸는 매퍼
public class ProfileStockMapper {

    public static RecommendationStock fromMap(Map<String, Object> map) {

        Double eps = toNullableDouble(map.get("eps"));
        Double bps = toNullableDouble(map.get("bps"));


        Double roe = (eps != null && bps != null && bps != 0.0)
                ? (eps / bps) * 100.0
                : null;
        return RecommendationStock.builder()
                .name((String) map.get("name"))
                .code((String) map.get("code"))
                .price(toNullableDouble(map.get("currentPrice")))
                .turnRate(toNullableDouble(map.get("turnoverRate")))
                .per(toNullableDouble(map.get("per")))
                .pbr(toNullableDouble(map.get("pbr")))
                .eps(toNullableDouble(map.get("eps")))
                .roe(roe)
                .volume(toNullableDouble(map.get("volume")))
                .avgPrice(toNullableDouble(map.get("avgPrice")))
                .foreignRate(toNullableDouble(map.get("foreignRate")))
                .high(toNullableDouble(map.get("high")))
                .low(toNullableDouble(map.get("low")))
                .open(toNullableDouble(map.get("open")))
                .high52w(toNullableDouble(map.get("high52w")))
                .low52w(toNullableDouble(map.get("low52w")))
                .build();

    }

    private static String toNullableString(Object value) {
        return (value == null || value.toString().isBlank()) ? null : value.toString();
    }

    private static Double toNullableDouble(Object value) {
        try {
            if (value == null || value.toString().isBlank()) return null;
            return Double.parseDouble(value.toString().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

}
