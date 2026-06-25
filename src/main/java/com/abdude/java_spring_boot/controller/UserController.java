package com.abdude.java_spring_boot.controller;

import com.abdude.java_spring_boot.dto.UserResponse;
import com.abdude.java_spring_boot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/user")
    public ResponseEntity<UserResponse> getAuthenticatedUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getAuthenticatedUser(authentication.getName()));
    }
}
