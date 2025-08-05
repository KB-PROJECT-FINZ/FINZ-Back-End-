package org.scoula.domain.redis.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChartResponse {
    private String rt_cd;
    private String msg_cd;
    private String msg1;
    private Output1 output1;
    private List<Output2> output2;
}
