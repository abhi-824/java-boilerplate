package com.abdude.java_spring_boot.service;

import com.abdude.java_spring_boot.dto.SignUpRequest;
import com.abdude.java_spring_boot.dto.UserResponse;
import com.abdude.java_spring_boot.entity.Role;
import com.abdude.java_spring_boot.entity.User;
import com.abdude.java_spring_boot.exception.DuplicateResourceException;
import com.abdude.java_spring_boot.exception.ResourceNotFoundException;
import com.abdude.java_spring_boot.repository.RoleRepository;
import com.abdude.java_spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerUser(SignUpRequest request) {
        if (userRepository.existsByUsernameOrEmail(request.getEmail(), request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role userRole = roleRepository.findByName(ROLE_USER);

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setUsername(request.getEmail());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.addRoles(userRole);

        userRepository.save(newUser);
    }

    public UserResponse getAuthenticatedUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
