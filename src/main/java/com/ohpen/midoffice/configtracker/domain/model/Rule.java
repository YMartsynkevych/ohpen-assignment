package com.ohpen.midoffice.configtracker.domain.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Rule.CreditLimitRule.class, name = "CREDIT_LIMIT"),
    @JsonSubTypes.Type(value = Rule.ApprovalPolicyRule.class, name = "APPROVAL_POLICY")
})
public sealed interface Rule {
    
    record CreditLimitRule(
        BigDecimal amount,
        String currency,
        String customerId
    ) implements Rule {}

    record ApprovalPolicyRule(
        String policyName,
        List<String> approvers,
        int requiredSignatures
    ) implements Rule {}
}
