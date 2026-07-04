package com.ohpen.midoffice.configtracker.domain.model;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ConfigChangeEvent extends ApplicationEvent {
    private final ConfigChange change;

    public ConfigChangeEvent(Object source, ConfigChange change) {
        super(source);
        this.change = change;
    }
}
