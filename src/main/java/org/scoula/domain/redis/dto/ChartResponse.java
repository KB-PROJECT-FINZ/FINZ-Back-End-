package org.scoula.domain.redis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ChartResponse {
    private String rt_cd;
    private String msg_cd;
    private String msg1;
    private List<Output2> output2;
}
