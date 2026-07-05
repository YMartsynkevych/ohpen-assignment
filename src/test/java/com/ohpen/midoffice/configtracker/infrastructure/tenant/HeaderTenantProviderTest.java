package com.ohpen.midoffice.configtracker.infrastructure.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeaderTenantProviderTest {

    private final HeaderTenantProvider headerTenantProvider = new HeaderTenantProvider();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldReturnTenantIdFromHeader() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HeaderTenantProvider.TENANT_HEADER)).thenReturn("tenant-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // When
        String tenantId = headerTenantProvider.getTenantId();

        // Then
        assertThat(tenantId).isEqualTo("tenant-123");
    }

    @Test
    void shouldReturnNullWhenHeaderIsMissing() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HeaderTenantProvider.TENANT_HEADER)).thenReturn(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // When
        String tenantId = headerTenantProvider.getTenantId();

        // Then
        assertThat(tenantId).isNull();
    }

    @Test
    void shouldReturnNullWhenNoRequestAttributes() {
        // Given
        RequestContextHolder.resetRequestAttributes();

        // When
        String tenantId = headerTenantProvider.getTenantId();

        // Then
        assertThat(tenantId).isNull();
    }
}
