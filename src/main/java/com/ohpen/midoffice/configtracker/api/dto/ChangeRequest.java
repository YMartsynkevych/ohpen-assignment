package com.ohpen.midoffice.configtracker.api.dto;

import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeRequest(
    @NotNull RuleType type,
    @NotNull ChangeOperation operation,
    @NotBlank String actor,
    Rule payload,        // for ADD or DELETE
    Rule oldPayload,     // for UPDATE
    Rule newPayload      // for UPDATE
) {}
