package com.ohpen.midoffice.configtracker.infrastructure.persistence;

import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RuleStateRepository extends JpaRepository<RuleStateEntity, Long> {
    Optional<RuleStateEntity> findByRuleTypeAndRuleKey(RuleType ruleType, String ruleKey);
}
