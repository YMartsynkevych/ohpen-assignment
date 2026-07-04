package com.ohpen.midoffice.configtracker.domain.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ConfigChange(
    UUID id,
    RuleType ruleType,
    ChangeOperation operation,
    RulePayload payload,
    LocalDateTime timestamp,
    String actor,
    Map<String, String> metadata
) {
    public ConfigChange {
        if (id == null) id = UUID.randomUUID();
        if (timestamp == null) timestamp = LocalDateTime.now();
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("Actor is required");
        if (ruleType == null) throw new IllegalArgumentException("Rule type is required");
        if (operation == null) throw new IllegalArgumentException("Operation is required");
        if (payload == null) throw new IllegalArgumentException("Payload is required");
    }
}
