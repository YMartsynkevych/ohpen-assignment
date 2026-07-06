package com.ohpen.midoffice.configtracker.api.rest;

import com.ohpen.midoffice.configtracker.api.dto.RuleStateResponse;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.domain.service.ConfigChangeService;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.RuleStateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleStateController {

    private final ConfigChangeService changeService;

    @GetMapping
    public List<RuleStateResponse> getRuleStates(@RequestParam(required = false) RuleType type) {
        return changeService.getRuleStatesByType(type).stream()
            .map(this::mapToResponse)
            .toList();
    }

    private RuleStateResponse mapToResponse(RuleStateEntity entity) {
        return new RuleStateResponse(
            entity.getId(),
            entity.getRuleType(),
            entity.getRuleKey(),
            entity.getPayloadJson(),
            entity.getLastModified()
        );
    }
}
