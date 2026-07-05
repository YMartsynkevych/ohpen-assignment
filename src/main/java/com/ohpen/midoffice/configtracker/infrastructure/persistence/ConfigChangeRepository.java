package com.ohpen.midoffice.configtracker.infrastructure.persistence;

import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConfigChangeRepository extends JpaRepository<ConfigChangeEntity, UUID> {
    @Query("SELECT c FROM ConfigChangeEntity c WHERE c.ruleType = ?1 AND c.tenantId = ?#{T(com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantContext).getTenantId()}")
    List<ConfigChangeEntity> findByRuleType(RuleType ruleType);

    @Query("SELECT c FROM ConfigChangeEntity c WHERE c.timestamp BETWEEN ?1 AND ?2 AND c.tenantId = ?#{T(com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantContext).getTenantId()}")
    List<ConfigChangeEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
