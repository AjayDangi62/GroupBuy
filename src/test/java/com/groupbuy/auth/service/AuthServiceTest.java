package com.groupbuy.auth.service;

import com.groupbuy.auth.dto.AuthResponse;
import com.groupbuy.auth.dto.LoginRequest;
import com.groupbuy.auth.dto.RefreshTokenRequest;
import com.groupbuy.auth.dto.RegisterRequest;
import com.groupbuy.auth.entity.RefreshToken;
import com.groupbuy.auth.security.JwtTokenProvider;
import com.groupbuy.common.exception.AccountSuspendedException;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.DuplicateResourceException;
import com.groupbuy.common.exception.InvalidCredentialsException;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, refreshTokenService);
    }

    @Test
    @DisplayName("register successfully creates user and returns AuthResponse")
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest(
                "bob@example.com",
                "bob_buyer",
                "SecureP@ss1",
                "Bob Builder",
                "CUSTOMER"
        );

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("bob_buyer")).thenReturn(false);
        when(passwordEncoder.encode("SecureP@ss1")).thenReturn("bcrypt_hash");

        User savedUser = new User("bob@example.com", "bob_buyer", "bcrypt_hash", "Bob Builder", User.Role.CUSTOMER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("mock_access_token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        RefreshToken mockRefreshToken = new RefreshToken(savedUser, "hash", Instant.now().plusSeconds(3600), "fam-1");
        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn(new RefreshTokenService.TokenPair("mock_raw_refresh_token", mockRefreshToken));

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock_access_token");
        assertThat(response.refreshToken()).isEqualTo("mock_raw_refresh_token");
        assertThat(response.role()).isEqualTo("CUSTOMER");
        assertThat(response.email()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("register throws DuplicateResourceException when email is already taken")
    void registerDuplicateEmailThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "newuser",
                "SecureP@ss1",
                "New User",
                "CUSTOMER"
        );

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email address is already in use");
    }

    @Test
    @DisplayName("register throws DuplicateResourceException when username is already taken")
    void registerDuplicateUsernameThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "new@example.com",
                "existing_user",
                "SecureP@ss1",
                "New User",
                "CUSTOMER"
        );

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    @DisplayName("register throws BusinessException on weak password")
    void registerWeakPasswordThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "weak@example.com",
                "weakuser",
                "simple", // fails complexity requirements
                "Weak User",
                "CUSTOMER"
        );

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Password must be at least 8 characters");
    }

    @Test
    @DisplayName("register rejects ADMIN self-registration")
    void registerAdminRoleForbidden() {
        RegisterRequest request = new RegisterRequest(
                "admin@example.com",
                "hacker_admin",
                "SecureP@ss1",
                "Hacker Admin",
                "ADMIN"
        );

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ADMIN is strictly prohibited");
    }

    @Test
    @DisplayName("login succeeds with valid credentials")
    void loginSuccess() {
        LoginRequest request = new LoginRequest("alice@example.com", "SecureP@ss1");
        User user = new User("alice@example.com", "alice", "bcrypt_hash", "Alice", User.Role.CUSTOMER);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecureP@ss1", "bcrypt_hash")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access_token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        RefreshToken mockToken = new RefreshToken(user, "hash", Instant.now().plusSeconds(3600), "fam-1");
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(new RefreshTokenService.TokenPair("refresh_token", mockToken));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access_token");
        assertThat(response.refreshToken()).isEqualTo("refresh_token");
    }

    @Test
    @DisplayName("login fails with wrong password")
    void loginWrongPasswordFails() {
        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword1!");
        User user = new User("alice@example.com", "alice", "bcrypt_hash", "Alice", User.Role.CUSTOMER);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword1!", "bcrypt_hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login fails for suspended user")
    void loginSuspendedUserFails() {
        LoginRequest request = new LoginRequest("suspended@example.com", "SecureP@ss1");
        User user = new User("suspended@example.com", "suspended", "bcrypt_hash", "Suspended", User.Role.CUSTOMER);
        user.setStatus(User.Status.SUSPENDED);

        when(userRepository.findByEmail("suspended@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecureP@ss1", "bcrypt_hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountSuspendedException.class);
    }

    @Test
    @DisplayName("refreshToken delegates to refreshTokenService and generates new tokens")
    void refreshTokenSuccess() {
        RefreshTokenRequest request = new RefreshTokenRequest("raw_refresh_token");
        User user = new User("alice@example.com", "alice", "hash", "Alice", User.Role.CUSTOMER);

        when(refreshTokenService.rotateRefreshToken("raw_refresh_token"))
                .thenReturn(new RefreshTokenService.TokenRotationResult("new_raw_token", user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new_access_token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo("new_access_token");
        assertThat(response.refreshToken()).isEqualTo("new_raw_token");
    }
}
