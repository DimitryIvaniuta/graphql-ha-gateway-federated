package com.github.dimitryivaniuta.gateway.persistence.repository;

import com.github.dimitryivaniuta.gateway.persistence.entity.PersistedQueryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersistedQueryRepository extends JpaRepository<PersistedQueryEntity, Long> {

    Optional<PersistedQueryEntity> findByQueryId(String queryId);
}
