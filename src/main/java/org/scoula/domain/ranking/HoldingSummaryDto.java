package org.scoula.domain.ranking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldingSummaryDto {
    private String stockCode;
    private String stockName;
    private double averagePrice;
}
