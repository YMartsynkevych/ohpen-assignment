package com.ohpen.midoffice.configtracker.domain;

import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigChangeTest {

    @Test
    void shouldCreateValidConfigChange() {
        Rule payload = new Rule.CreditLimitRule(new BigDecimal("100.00"), "EUR", "CUST-1");
        ConfigChange change = new ConfigChange.AddedRule(
            UUID.randomUUID(),
            LocalDateTime.now(),
            "admin",
            RuleType.CREDIT_LIMIT,
            payload
        );

        assertNotNull(change.id());
        assertEquals("admin", change.actor());
        assertEquals(RuleType.CREDIT_LIMIT, change.ruleType());
    }

    @Test
    void shouldCreateUpdatedRule() {
        Rule oldRule = new Rule.CreditLimitRule(new BigDecimal("100.00"), "EUR", "CUST-1");
        Rule newRule = new Rule.CreditLimitRule(new BigDecimal("200.00"), "EUR", "CUST-1");
        
        ConfigChange.UpdatedRule change = new ConfigChange.UpdatedRule(
            UUID.randomUUID(),
            LocalDateTime.now(),
            "admin",
            RuleType.CREDIT_LIMIT,
            oldRule,
            newRule
        );

        assertEquals(oldRule, change.oldRule());
        assertEquals(newRule, change.newRule());
    }
}
