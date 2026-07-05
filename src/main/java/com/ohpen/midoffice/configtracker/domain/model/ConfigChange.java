package com.ohpen.midoffice.configtracker.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public sealed interface ConfigChange {
    UUID id();
    LocalDateTime timestamp();
    String actor();
    String tenantId();
    RuleType ruleType();

    record AddedRule(
        UUID id,
        LocalDateTime timestamp,
        String actor,
        String tenantId,
        RuleType ruleType,
        Rule newRule
    ) implements ConfigChange {}

    record UpdatedRule(
        UUID id,
        LocalDateTime timestamp,
        String actor,
        String tenantId,
        RuleType ruleType,
        Rule oldRule,
        Rule newRule
    ) implements ConfigChange {}

    record RemovedRule(
        UUID id,
        LocalDateTime timestamp,
        String actor,
        String tenantId,
        RuleType ruleType,
        Rule removedRule
    ) implements ConfigChange {}
}
