package org.scoula.util.chatbot;

import org.scoula.domain.chatbot.dto.ChatAnalysisDto;
import org.scoula.domain.chatbot.dto.RecommendationStock;

import java.time.LocalDateTime;

public class ChatAnalysisMapper {

    public static ChatAnalysisDto toDto(RecommendationStock stock) {

        System.out.println("🧪 stock code: " + stock.getCode());
        return ChatAnalysisDto.builder()
                .ticker(stock.getCode())// 추출하거나 매핑 로직 있어야 함
                .name(stock.getName())
                .region("KR") // 기본값, 조건에 따라 US 넣을 수 있음
                .per(parseFloat(stock.getPer()))
                .roe(null) // 따로 데이터 없으면 일단 null
                .eps(parseFloat(stock.getEps()))
                .price(parseFloat(stock.getPrice()))
                .pbr(parseFloat(stock.getPbr()))
                .open(parseFloat(stock.getOpen()))
                .high(parseFloat(stock.getHigh()))
                .low(parseFloat(stock.getLow()))
                .volume(parseLong(stock.getVolume()))
                .avgPrice(parseFloat(stock.getAvgPrice()))
                .foreignRate(parseFloat(stock.getForeignRate()))
                .turnRate(parseFloat(stock.getTurnRate()))
                .high52w(parseFloat(stock.getHigh52w()))
                .low52w(parseFloat(stock.getLow52w()))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private static Float parseFloat(String val) {
        try {
            return val == null || val.isBlank() ? null : Float.parseFloat(val.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parseLong(String val) {
        try {
            return val == null || val.isBlank() ? null : Long.parseLong(val.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }
}