package com.ohpen.midoffice.configtracker.api.rest;

import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.api.dto.ChangeResponse;
import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.domain.service.ConfigChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/changes")
@RequiredArgsConstructor
public class ChangeController {

    private final ConfigChangeService changeService;

    @PostMapping
    public ResponseEntity<ChangeResponse> createChange(@Valid @RequestBody ChangeRequest request) {
        ConfigChange saved = changeService.processChangeRequest(request);
        return ResponseEntity.ok(mapToResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChangeResponse> getChangeById(@PathVariable UUID id) {
        return changeService.getChangeById(id)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Change not found with id: " + id));
    }

    @GetMapping
    public List<ChangeResponse> getChanges(
        @RequestParam(required = false) RuleType type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        if (type != null) {
            return changeService.getChangesByType(type).stream().map(this::mapToResponse).toList();
        }
        if (from != null && to != null) {
            return changeService.getChangesByTimeRange(from, to).stream().map(this::mapToResponse).toList();
        }
        return changeService.getAllChanges().stream().map(this::mapToResponse).toList();
    }

    private ChangeResponse mapToResponse(ConfigChange change) {
        return switch (change) {
            case ConfigChange.AddedRule added -> new ChangeResponse(
                added.id(), added.ruleType(), ChangeOperation.ADD, added.actor(), added.timestamp(), added.newRule(), null, null
            );
            case ConfigChange.UpdatedRule updated -> new ChangeResponse(
                updated.id(), updated.ruleType(), ChangeOperation.UPDATE, updated.actor(), updated.timestamp(), null, updated.oldRule(), updated.newRule()
            );
            case ConfigChange.RemovedRule removed -> new ChangeResponse(
                removed.id(), removed.ruleType(), ChangeOperation.DELETE, removed.actor(), removed.timestamp(), removed.removedRule(), null, null
            );
        };
    }
}
