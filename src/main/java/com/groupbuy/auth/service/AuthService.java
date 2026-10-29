package com.groupbuy.auth.service;

import com.groupbuy.auth.dto.AuthResponse;
import com.groupbuy.auth.dto.LoginRequest;
import com.groupbuy.auth.dto.RefreshTokenRequest;
import com.groupbuy.auth.dto.RegisterRequest;
import com.groupbuy.auth.security.JwtTokenProvider;
import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.common.exception.AccountSuspendedException;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.DuplicateResourceException;
import com.groupbuy.common.exception.InvalidCredentialsException;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Authentication service handling user registration, credential verification,
 * token pair generation, token rotation, and revocation.
 */
@Service
public class AuthService {

    // Regex requiring at least 1 uppercase, 1 lowercase, 1 digit, 1 special character, min 8 chars
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^~`+=\\-\\[\\]{}()|:;\"'<>,./]).{8,}$"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Registers a new user, hashes password with BCrypt, stores record, and issues initial token pair.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String trimmedUsername = request.username().trim();

        // Check for duplicate email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "Email address is already in use: " + normalizedEmail,
                    "EMAIL_ALREADY_EXISTS"
            );
        }

        // Check for duplicate username
        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new DuplicateResourceException(
                    "Username is already taken: " + trimmedUsername,
                    "USERNAME_ALREADY_EXISTS"
            );
        }

        // Validate password complexity
        validatePasswordComplexity(request.password());

        // Parse and validate requested role
        User.Role role = determineRegistrationRole(request.role());

        // Hash password with BCrypt (strength 12)
        String hashedPassword = passwordEncoder.encode(request.password());

        User user = new User(
                normalizedEmail,
                trimmedUsername,
                hashedPassword,
                request.fullName().trim(),
                role
        );
        User savedUser = userRepository.save(user);

        // Issue initial token pair
        UserPrincipal principal = UserPrincipal.fromUser(savedUser);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        RefreshTokenService.TokenPair refreshPair = refreshTokenService.createRefreshToken(savedUser);

        return AuthResponse.of(
                accessToken,
                refreshPair.rawToken(),
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                savedUser.getFullName(),
                savedUser.getRole().name()
        );
    }

    /**
     * Authenticates user credentials and issues a fresh token pair with a new rotation family.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() == User.Status.SUSPENDED) {
            throw new AccountSuspendedException("Account has been suspended. Please contact customer support.");
        }
        if (user.getStatus() == User.Status.DELETED) {
            throw new AccountSuspendedException("Account has been deactivated.");
        }

        UserPrincipal principal = UserPrincipal.fromUser(user);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        RefreshTokenService.TokenPair refreshPair = refreshTokenService.createRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshPair.rawToken(),
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    /**
     * Rotates refresh token and issues a new access token and successor refresh token.
     */
    @Transactional(noRollbackFor = com.groupbuy.common.exception.TokenRefreshException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenService.TokenRotationResult result = refreshTokenService.rotateRefreshToken(request.refreshToken());
        User user = result.user();

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new AccountSuspendedException("User account is no longer active.");
        }

        UserPrincipal principal = UserPrincipal.fromUser(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);

        return AuthResponse.of(
                newAccessToken,
                result.newRawToken(),
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    /**
     * Revokes the provided refresh token on logout.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
    }

    private void validatePasswordComplexity(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(
                    "Password must be at least 8 characters long and contain at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character.",
                    "PASSWORD_COMPLEXITY_NOT_MET"
            );
        }
    }

    private User.Role determineRegistrationRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return User.Role.CUSTOMER;
        }

        String uppercase = requestedRole.trim().toUpperCase();
        if (uppercase.equals("ADMIN")) {
            throw new BusinessException("Self-registration as ADMIN is strictly prohibited.", "ADMIN_REGISTRATION_FORBIDDEN");
        }

        try {
            return User.Role.valueOf(uppercase);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid role specified: " + requestedRole + ". Allowed roles: CUSTOMER, SELLER.", "INVALID_ROLE");
        }
    }
}
