package com.abdude.java_spring_boot.controller;

import com.abdude.java_spring_boot.dto.SignInRequest;
import com.abdude.java_spring_boot.dto.SignInResponse;
import com.abdude.java_spring_boot.dto.SignUpRequest;
import com.abdude.java_spring_boot.dto.SignUpResponse;
import com.abdude.java_spring_boot.service.AuthService;
import com.abdude.java_spring_boot.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) {
        userService.registerUser(request);
        String token = authService.generateTokenForEmail(request.getEmail());
        return ResponseEntity.ok(new SignUpResponse(token));
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signin(@Valid @RequestBody SignInRequest request) {
        return ResponseEntity.ok(new SignInResponse(authService.authUser(request)));
    }
}
