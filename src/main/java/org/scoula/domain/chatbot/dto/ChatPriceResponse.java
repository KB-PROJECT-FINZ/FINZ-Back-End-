package org.scoula.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;



// API 응답 원본 DTO
@Data
public class ChatPriceResponse {

    private String rt_cd;
    private String msg_cd;
    private String msg1;
    private Output output;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true) // 필요없는 값 무시
    public static class Output {
        private String stck_prpr;              // 현재가
        private String per;                    // PER
        private String eps;                    // EPS
        private String pbr;                    // PBR
        private String stck_oprc;              // 시가
        private String stck_hgpr;              // 고가
        private String stck_lwpr;              // 저가
        private String acml_vol;               // 누적 거래량
        private String wghn_avrg_stck_prc;     // 가중 평균 주가
        private String hts_frgn_ehrt;          // 외국인 지분율
        private String vol_tnrt;               // 거래 회전율
        private String d250_hgpr;              // 52주 고가
        private String d250_lwpr;              // 52주 저가
    }
}
