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
    @JsonSubTypes.Type(value = RulePayload.CreditLimitPayload.class, name = "CREDIT_LIMIT"),
    @JsonSubTypes.Type(value = RulePayload.ApprovalPolicyPayload.class, name = "APPROVAL_POLICY")
})
public sealed interface RulePayload {
    
    record CreditLimitPayload(
        BigDecimal amount,
        String currency,
        String customerId
    ) implements RulePayload {}

    record ApprovalPolicyPayload(
        String policyName,
        List<String> approvers,
        int requiredSignatures
    ) implements RulePayload {}
}
