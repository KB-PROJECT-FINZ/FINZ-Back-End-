package org.scoula.domain.Auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVo {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private String nickname;
    private Integer profileImage;
    private String createdAt;
    private String updatedAt;
    private String riskType;
    private String phoneNumber;
    private String email;
    private String provider;
    private Long totalCredit;
}
