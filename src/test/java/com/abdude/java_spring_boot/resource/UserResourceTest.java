package com.abdude.java_spring_boot.resource;

import com.abdude.java_spring_boot.dto.UserResponse;
import com.abdude.java_spring_boot.entity.User;
import com.abdude.java_spring_boot.exception.ResourceNotFoundException;
import com.abdude.java_spring_boot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserResource userResource;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_NAME = "Test User";
    private static final Long TEST_ID = 1L;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_ID);
        testUser.setName(TEST_NAME);
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail(TEST_EMAIL);
        testUser.setPassword("encodedPassword");
    }

    @Test
    void getAuthenticatedUser_happyPath_returnsUserResponse() {
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        ResponseEntity<UserResponse> response = userResource.getAuthenticatedUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UserResponse body = response.getBody();
        assertThat(body.getId()).isEqualTo(TEST_ID);
        assertThat(body.getName()).isEqualTo(TEST_NAME);
        assertThat(body.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(body.getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    void getAuthenticatedUser_userNotFound_throwsResourceNotFoundException() {
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userResource.getAuthenticatedUser(authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("email")
                .hasMessageContaining(TEST_EMAIL);
    }
}
