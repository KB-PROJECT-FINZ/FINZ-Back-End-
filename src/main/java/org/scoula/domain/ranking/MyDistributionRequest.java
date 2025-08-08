package org.scoula.domain.ranking;

import lombok.Data;

import java.util.List;

@Data
public class MyDistributionRequest {
    private Integer userId;
    private List<MyDistributionDto> distributions;
}
