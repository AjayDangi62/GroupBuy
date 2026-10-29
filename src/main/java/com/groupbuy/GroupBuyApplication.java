package com.groupbuy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class GroupBuyApplication {

    public static void main(String[] args) {
        // Enforce UTC timezone across JVM for predictable timestamp operations
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(GroupBuyApplication.class, args);
    }
}
