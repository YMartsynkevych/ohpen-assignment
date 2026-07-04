package com.ohpen.midoffice.configtracker.infrastructure.persistence;

import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConfigChangeRepository extends JpaRepository<ConfigChangeEntity, UUID> {
    List<ConfigChangeEntity> findByRuleType(RuleType ruleType);
    List<ConfigChangeEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
