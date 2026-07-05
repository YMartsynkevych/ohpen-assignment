package com.ohpen.midoffice.configtracker.infrastructure.tenant;

import org.springframework.stereotype.Component;

@Component
public class JwtTenantProvider implements TenantProvider {
    @Override
    public String getTenantId() {
        return null; // Implementation pending dependency resolution
    }
}
