package com.ohpen.midoffice.configtracker.config;

import com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebConfigTest {

    @Test
    void shouldAddTenantInterceptor() {
        // Given
        TenantInterceptor tenantInterceptor = mock(TenantInterceptor.class);
        WebConfig webConfig = new WebConfig(tenantInterceptor);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        // When
        webConfig.addInterceptors(registry);

        // Then
        verify(registry).addInterceptor(tenantInterceptor);
    }
}
