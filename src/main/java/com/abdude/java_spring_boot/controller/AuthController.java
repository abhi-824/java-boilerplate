package com.abdude.java_spring_boot.controller;

import com.abdude.java_spring_boot.dto.SignInRequest;
import com.abdude.java_spring_boot.dto.SignInResponse;
import com.abdude.java_spring_boot.dto.SignUpResponse;
import com.abdude.java_spring_boot.repository.UserRepository;
import com.abdude.java_spring_boot.security.CustomUserDetailsService;
import com.abdude.java_spring_boot.security.jwt.JwtTokenProvider;
import com.abdude.java_spring_boot.service.AuthService;
import com.abdude.java_spring_boot.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.abdude.java_spring_boot.dto.SignUpRequest;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@RequestBody SignUpRequest request) {
        userService.registerUser(request);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                request.getEmail(), null, new ArrayList<>());

        String jwtToken = jwtTokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new SignUpResponse(jwtToken));
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signin(@RequestBody SignInRequest request) {
        return ResponseEntity.ok(new SignInResponse(
                authService.authUser(request)
        ));
    }
}
