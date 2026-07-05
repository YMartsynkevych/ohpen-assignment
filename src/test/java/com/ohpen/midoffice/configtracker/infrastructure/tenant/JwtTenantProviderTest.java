package com.ohpen.midoffice.configtracker.infrastructure.tenant;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtTenantProviderTest {

    private final JwtTenantProvider jwtTenantProvider = new JwtTenantProvider();

    @Test
    void shouldReturnNullForTenantId() {
        // As per current implementation which is pending dependency resolution
        assertThat(jwtTenantProvider.getTenantId()).isNull();
    }
}
