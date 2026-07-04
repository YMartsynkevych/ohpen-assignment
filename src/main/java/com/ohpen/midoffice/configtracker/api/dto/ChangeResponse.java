package com.ohpen.midoffice.configtracker.api.dto;

import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.RulePayload;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChangeResponse(
    UUID id,
    RuleType type,
    ChangeOperation operation,
    String actor,
    LocalDateTime timestamp,
    RulePayload payload
) {}
