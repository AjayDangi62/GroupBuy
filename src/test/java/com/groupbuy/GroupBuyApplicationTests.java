package com.groupbuy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Context Load Test")
class GroupBuyApplicationTests {

    @Test
    @DisplayName("Spring ApplicationContext loads successfully with core beans")
    void contextLoads() {
        // Confirms that component scanning, configuration properties, security, and pricing beans initialize cleanly
        assertThat(true).isTrue();
    }
}
