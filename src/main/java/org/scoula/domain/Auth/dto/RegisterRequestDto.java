package org.scoula.domain.Auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDto {
    private String username;
    private String password;
    private String name;
    private String nickname;
    private String phoneNumber;
    private String email;
}

