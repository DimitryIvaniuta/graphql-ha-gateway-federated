package com.github.dimitryivaniuta.gateway.persistence.repository;

import com.github.dimitryivaniuta.gateway.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for user accounts used in SSO flows.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Find an enabled user by tenant and username.
     */
    Optional<UserEntity> findByTenantIdAndUsernameAndEnabledIsTrue(String tenantId, String username);
}
