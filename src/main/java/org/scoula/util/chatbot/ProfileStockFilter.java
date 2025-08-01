    package org.scoula.util.chatbot;

    import lombok.extern.log4j.Log4j2;
    import org.scoula.domain.chatbot.dto.RecommendationStock;

    import java.util.List;
    import java.util.stream.Collectors;

    @Log4j2
    public class ProfileStockFilter {


        public static List<RecommendationStock> filterByRiskType(String riskType, List<RecommendationStock> stocks) {
            return stocks.stream()
                    .filter(stock -> isMatch(riskType, stock))
                    .collect(Collectors.toList());
        }

        public static boolean isMatch(String riskType, RecommendationStock stock) {
            Float per = toFloat(stock.getPer());
            Float pbr = toFloat(stock.getPbr());
            Float volume = toFloat(stock.getVolume());
            Float turnRate = toFloat(stock.getTurnRate());

            log.info("🧪 [{}] per={}, pbr={}, volume={}, turnRate={}", riskType, per, pbr, volume, turnRate);
            log.info("🧪 [{}] 종목={}, 원본 per={}, 원본 pbr={}, 원본 volume={}, 원본 turnRate={}",
                    riskType, stock.getName(), stock.getPer(), stock.getPbr(), stock.getVolume(), stock.getTurnRate());

            switch (riskType) {
                case "AGR": // 적극적 성장형
                case "DTA": // 단타 추구형
                case "THE": // 테마 투자형
                    return turnRate != null && turnRate > 1.0;

                case "VAL": // 가치 투자형
                    return per != null && per < 15 &&
                            pbr != null && pbr < 1.5;

                case "TEC": // 기술적 분석형
                    return volume != null && volume > 500_000;

                case "IND": // 인덱스 수동형
                case "CSD": // 신중한 안정형
                    return per != null && per < 20 &&
                            pbr != null && pbr < 2.0 &&
                            turnRate != null && turnRate > 0.3;

                case "INF": // 정보 수집형
                case "SYS": // 시스템 트레이더형
                    return per != null && per < 40 &&
                            turnRate != null && turnRate > 0.2;

                default:
                    return true;
            }
        }

        private static Float parseFloat(String val) {
            try {
                if (val == null || val.isBlank() || val.equalsIgnoreCase("null")) return null;
                return Float.parseFloat(val.replace(",", ""));
            } catch (Exception e) {
                log.warn("⚠️ parseFloat 실패 → val: {}", val);
                return null;
            }
        }
        private static Float toFloat(Double val) {
            return val == null ? null : val.floatValue();
        }
    }