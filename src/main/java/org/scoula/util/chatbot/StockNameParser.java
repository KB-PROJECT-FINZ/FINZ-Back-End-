package org.scoula.util.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.StockExtractionResultDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class StockNameParser  {

    // 파싱함수
    public StockExtractionResultDto parseStockExtraction(String gptResponse) {
        String stockName = null;
        String ticker = null;

        for (String line : gptResponse.split("\\R")) {
            if (line.toLowerCase().startsWith("stock:")) {
                stockName = line.substring("stock:".length()).trim();
            } else if (line.toLowerCase().startsWith("ticker:")) {
                ticker = line.substring("ticker:".length()).trim();
                if ("null".equalsIgnoreCase(ticker)) {
                    ticker = null;
                }
            }
        }

        return new StockExtractionResultDto(stockName, ticker);
    }
}

