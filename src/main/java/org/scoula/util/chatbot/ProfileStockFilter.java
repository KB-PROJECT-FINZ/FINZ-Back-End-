package org.scoula.util.chatbot;

import org.scoula.domain.chatbot.dto.RecommendationStock;

import java.util.List;
import java.util.stream.Collectors;

public class ProfileStockFilter {

    public static List<RecommendationStock> filterByRiskType(String riskType, List<RecommendationStock> stocks) {
        return stocks.stream()
                .filter(stock -> isMatch(riskType, stock))
                .collect(Collectors.toList());
    }

    public static boolean isMatch(String riskType, RecommendationStock stock) {
        switch (riskType) {
            case "AGR": // 적극적 성장형
            case "DTA": // 단타 추구형
            case "THE": // 테마 투자형
                return parseFloat(stock.getTurnRate()) != null &&
                        parseFloat(stock.getTurnRate()) > 2.0;

            case "VAL": // 가치 투자형
                return parseFloat(stock.getPer()) != null && parseFloat(stock.getPer()) < 10 &&
                        parseFloat(stock.getPbr()) != null && parseFloat(stock.getPbr()) < 1.2;

            case "TEC": // 기술적 분석형
                return parseFloat(stock.getVolume()) != null && parseFloat(stock.getVolume()) > 1000000;

            case "IND": // 인덱스 수동형
            case "CSD": // 신중한 안정형
                return false;

            case "INF": // 정보 수집형
            case "SYS": // 시스템 트레이더형
                return parseFloat(stock.getPer()) != null && parseFloat(stock.getPer()) < 30 &&
                        parseFloat(stock.getTurnRate()) != null && parseFloat(stock.getTurnRate()) > 0.5;

            default:
                return true;
        }
    }

    private static Float parseFloat(String val) {
        try {
            return val == null || val.isBlank() ? null : Float.parseFloat(val.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
