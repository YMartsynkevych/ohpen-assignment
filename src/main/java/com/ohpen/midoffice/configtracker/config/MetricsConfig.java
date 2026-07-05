package com.ohpen.midoffice.configtracker.config;

import com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantContext;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterFilter tenantMeterFilter() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                String tenantId = TenantContext.getTenantId();
                if (tenantId != null) {
                    return id.withTag(Tag.of("tenant_id", tenantId));
                }
                return id;
            }
        };
    }
}
