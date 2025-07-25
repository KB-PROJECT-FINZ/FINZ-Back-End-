package org.scoula.util.mocktrading;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class CandleAggregator {

    public static class Candle {
        public String startTime;
        public String open;
        public String high;
        public String low;
        public String close;
        public String volume;

        public Candle(String startTime, String open, String high, String low, String close, String volume) {
            this.startTime = startTime;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }

        @Override
        public String toString() {
            return String.format("[%s] 시가:%s 고가:%s 저가:%s 종가:%s 거래량:%s",
                    startTime, open, high, low, close, volume);
        }
    }

    public static List<Candle> aggregate(JsonNode oneMinuteData, int unitMinutes) {
        List<Candle> aggregated = new ArrayList<>();

        int count = 0;
        String open = null, high = null, low = null, close = null;
        int totalVolume = 0;
        String startTime = "";

        for (JsonNode node : oneMinuteData) {
            String time = node.path("stck_cntg_hour").asText();  // HHMMSS
            String o = node.path("stck_oprc").asText();
            String h = node.path("stck_hgpr").asText();
            String l = node.path("stck_lwpr").asText();
            String c = node.path("stck_prpr").asText();
            int v = Integer.parseInt(node.path("cntg_vol").asText());

            if (count == 0) {
                open = o;
                high = h;
                low = l;
                startTime = time;
            } else {
                if (Integer.parseInt(h) > Integer.parseInt(high)) high = h;
                if (Integer.parseInt(l) < Integer.parseInt(low)) low = l;
            }

            close = c;
            totalVolume += v;
            count++;

            if (count == unitMinutes) {
                aggregated.add(new Candle(startTime, open, high, low, close, String.valueOf(totalVolume)));
                count = 0;
                totalVolume = 0;
            }
        }

        // 남은 데이터 처리
        if (count > 0) {
            aggregated.add(new Candle(startTime, open, high, low, close, String.valueOf(totalVolume)));
        }

        return aggregated;
    }
}
