package com.ohpen.midoffice.configtracker.infrastructure.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HeaderTenantProvider implements TenantProvider {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public String getTenantId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader(TENANT_HEADER))
                .orElse(null);
    }
}
