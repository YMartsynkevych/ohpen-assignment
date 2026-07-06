package com.ohpen.midoffice.configtracker.infrastructure.persistence;

import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface RuleStateRepository extends JpaRepository<RuleStateEntity, Long> {
    @Query("SELECT r FROM RuleStateEntity r WHERE r.ruleType = ?1 AND r.ruleKey = ?2 AND r.tenantId = ?#{T(com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantContext).getTenantId()}")
    Optional<RuleStateEntity> findByRuleTypeAndRuleKey(RuleType ruleType, String ruleKey);

    @Query("SELECT r FROM RuleStateEntity r WHERE r.ruleType = ?1 AND r.tenantId = ?#{T(com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantContext).getTenantId()}")
    List<RuleStateEntity> findByRuleType(RuleType ruleType);

    @Query("SELECT r FROM RuleStateEntity r WHERE r.tenantId = ?#{T(com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantContext).getTenantId()}")
    List<RuleStateEntity> findAllByTenantId();
}
