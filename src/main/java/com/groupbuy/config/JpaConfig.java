package com.groupbuy.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Persistence configuration.
 * Ensures consistent UTC timezone application across database sessions.
 */
@Configuration
public class JpaConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
