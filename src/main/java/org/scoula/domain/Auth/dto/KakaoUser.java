package org.scoula.domain.Auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor // ✅ 이거 추가!
@Builder
public class KakaoUser {
    private String id;
    private String nickname;
    private String email;
}
