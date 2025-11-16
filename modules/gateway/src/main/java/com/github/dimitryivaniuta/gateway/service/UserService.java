package com.github.dimitryivaniuta.gateway.service;

import com.github.dimitryivaniuta.gateway.persistence.entity.UserEntity;
import com.github.dimitryivaniuta.gateway.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Domain service for user management and credential-based authentication.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Look up users by tenant and username.</li>
 *   <li>Verify raw password using a {@link PasswordEncoder}.</li>
 *   <li>Update last-login information.</li>
 * </ul>
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate a user by tenant, username and raw password.
     *
     * @param tenantId   tenant identifier (cannot be null/blank)
     * @param username   login name (cannot be null/blank)
     * @param rawPassword user-supplied password in plain text
     * @return authenticated user, or empty if credentials are invalid
     */
    @Transactional
    public Optional<UserEntity> authenticate(String tenantId, String username, String rawPassword) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return Optional.empty();
        }

        Optional<UserEntity> userOpt = userRepository.findByTenantIdAndUsernameAndEnabledIsTrue(
                tenantId.trim(), username.trim());

        if (userOpt.isEmpty()) {
            log.debug("Authentication failed: no enabled user for tenant='{}', username='{}'", tenantId, username);
            return Optional.empty();
        }

        UserEntity user = userOpt.get();

        if (user.isLocked()) {
            log.debug("Authentication failed: user locked tenant='{}', username='{}'", tenantId, username);
            return Optional.empty();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.debug("Authentication failed: invalid password for tenant='{}', username='{}'", tenantId, username);
            return Optional.empty();
        }

        user.setLastLoginAt(OffsetDateTime.now());
        // flush lastLoginAt but keep transaction lightweight
        userRepository.save(user);

        log.debug("Authentication successful for tenant='{}', username='{}'", tenantId, username);
        return Optional.of(user);
    }

    /**
     * Helper for registration flows (if you decide to add them).
     */
    @Transactional
    public UserEntity createUser(String tenantId, String username, String rawPassword, String roles) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        UserEntity user = UserEntity.builder()
                .tenantId(tenantId.trim())
                .username(username.trim())
                .passwordHash(encodedPassword)
                .roles(roles != null && !roles.isBlank() ? roles : "ROLE_USER")
                .enabled(true)
                .locked(false)
                .build();
        return userRepository.save(user);
    }
}
