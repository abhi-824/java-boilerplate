package com.abdude.java_spring_boot.resource;

import com.abdude.java_spring_boot.dto.UserResponse;
import com.abdude.java_spring_boot.entity.User;
import com.abdude.java_spring_boot.exception.ResourceNotFoundException;
import com.abdude.java_spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserResource {

    private final UserRepository userRepository;

    @GetMapping("/user")
    public ResponseEntity<UserResponse> getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();

        return ResponseEntity.ok(response);
    }
}
