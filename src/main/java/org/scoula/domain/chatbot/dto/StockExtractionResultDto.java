package org.scoula.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockExtractionResultDto {
    private String stockName;
    private String ticker;
}


