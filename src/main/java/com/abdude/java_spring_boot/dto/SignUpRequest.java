package com.abdude.java_spring_boot.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SignUpRequest {
    private String name;
    private String email;
    private String password;
}
