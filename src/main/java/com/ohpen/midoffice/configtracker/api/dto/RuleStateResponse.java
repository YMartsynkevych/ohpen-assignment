package com.ohpen.midoffice.configtracker.api.dto;

import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import java.time.LocalDateTime;

public record RuleStateResponse(
    Long id,
    RuleType ruleType,
    String ruleKey,
    String payloadJson,
    LocalDateTime lastModified
) {}
