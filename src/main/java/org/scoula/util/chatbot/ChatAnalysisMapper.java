package org.scoula.util.chatbot;

import org.scoula.domain.chatbot.dto.ChatAnalysisDto;
import org.scoula.domain.chatbot.dto.RecommendationStock;

import java.time.LocalDateTime;

public class ChatAnalysisMapper {

    public static ChatAnalysisDto toDto(RecommendationStock stock) {

        System.out.println("🧪 stock code: " + stock.getCode());
        return ChatAnalysisDto.builder()
                .ticker(stock.getCode())
                .name(stock.getName())
                .region("KR")
                .per(toFloat(stock.getPer()))
                .roe(toFloat(stock.getRoe()))
                .eps(toFloat(stock.getEps()))
                .price(toFloat(stock.getPrice()))
                .pbr(toFloat(stock.getPbr()))
                .open(toFloat(stock.getOpen()))
                .high(toFloat(stock.getHigh()))
                .low(toFloat(stock.getLow()))
                .volume(toLong(stock.getVolume()))
                .avgPrice(toFloat(stock.getAvgPrice()))
                .foreignRate(toFloat(stock.getForeignRate()))
                .turnRate(toFloat(stock.getTurnRate()))
                .high52w(toFloat(stock.getHigh52w()))
                .low52w(toFloat(stock.getLow52w()))
                .updatedAt(LocalDateTime.now())
                .build();
    }


    private static Float toFloat(Double value) {
        return value == null ? null : value.floatValue();
    }

    private static Long toLong(Double value) {
        return value == null ? null : value.longValue();
    }
}