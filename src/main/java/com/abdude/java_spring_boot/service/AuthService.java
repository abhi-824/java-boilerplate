package com.abdude.java_spring_boot.service;

import com.abdude.java_spring_boot.dto.SignInRequest;
import com.abdude.java_spring_boot.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public String authUser(SignInRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        return jwtTokenProvider.generateToken(authentication);
    }

    public String generateTokenForEmail(String email) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null, new ArrayList<>());
        return jwtTokenProvider.generateToken(authentication);
    }
}
