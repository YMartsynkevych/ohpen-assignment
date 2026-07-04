package com.ohpen.midoffice.configtracker.api.rest;

import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.api.dto.ChangeResponse;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.domain.service.ChangeTrackerService;
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

    private final ChangeTrackerService changeService;

    @PostMapping
    public ResponseEntity<ChangeResponse> createChange(@Valid @RequestBody ChangeRequest request) {
        ConfigChange change = new ConfigChange(
            UUID.randomUUID(),
            request.type(),
            request.operation(),
            request.payload(),
            LocalDateTime.now(),
            request.actor(),
            null
        );
        ConfigChange saved = changeService.trackChange(change);
        return ResponseEntity.ok(mapToResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChangeResponse> getChangeById(@PathVariable UUID id) {
        return changeService.getChangeById(id)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
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
        // Simplified: return empty or default behavior
        return List.of();
    }

    private ChangeResponse mapToResponse(ConfigChange change) {
        return new ChangeResponse(
            change.id(),
            change.ruleType(),
            change.operation(),
            change.actor(),
            change.timestamp(),
            change.payload()
        );
    }
}
