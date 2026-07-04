package com.ohpen.midoffice.configtracker.domain;

import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.RulePayload;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigChangeTest {

    @Test
    void shouldCreateValidConfigChange() {
        RulePayload payload = new RulePayload.CreditLimitPayload(new BigDecimal("100.00"), "EUR", "CUST-1");
        ConfigChange change = new ConfigChange(
            UUID.randomUUID(),
            RuleType.CREDIT_LIMIT,
            ChangeOperation.ADD,
            payload,
            LocalDateTime.now(),
            "admin",
            null
        );

        assertNotNull(change.id());
        assertEquals("admin", change.actor());
        assertEquals(RuleType.CREDIT_LIMIT, change.ruleType());
    }

    @Test
    void shouldThrowExceptionWhenActorIsBlank() {
        RulePayload payload = new RulePayload.CreditLimitPayload(new BigDecimal("100.00"), "EUR", "CUST-1");
        assertThrows(IllegalArgumentException.class, () -> 
            new ConfigChange(null, RuleType.CREDIT_LIMIT, ChangeOperation.ADD, payload, null, "", null)
        );
    }
}
