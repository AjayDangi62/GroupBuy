package com.groupbuy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestDatabaseCleaner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void clean() {
        try {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
            jdbcTemplate.execute("TRUNCATE TABLE payment_attempts");
            jdbcTemplate.execute("TRUNCATE TABLE payment_events");
            jdbcTemplate.execute("TRUNCATE TABLE refunds");
            jdbcTemplate.execute("TRUNCATE TABLE payments");
            jdbcTemplate.execute("TRUNCATE TABLE order_items");
            jdbcTemplate.execute("TRUNCATE TABLE orders");
            jdbcTemplate.execute("TRUNCATE TABLE cart_items");
            jdbcTemplate.execute("TRUNCATE TABLE carts");
            jdbcTemplate.execute("TRUNCATE TABLE inventory_reservations");
            jdbcTemplate.execute("TRUNCATE TABLE participations");
            jdbcTemplate.execute("TRUNCATE TABLE pricing_tiers");
            jdbcTemplate.execute("TRUNCATE TABLE campaigns");
            jdbcTemplate.execute("TRUNCATE TABLE products");
            jdbcTemplate.execute("TRUNCATE TABLE categories");
            jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens");
            jdbcTemplate.execute("TRUNCATE TABLE users");
        } catch (Exception ex) {
            // Fallback for non-H2 if executed in other environments
            try {
                jdbcTemplate.execute("DELETE FROM payment_attempts");
                jdbcTemplate.execute("DELETE FROM payment_events");
                jdbcTemplate.execute("DELETE FROM refunds");
                jdbcTemplate.execute("DELETE FROM payments");
                jdbcTemplate.execute("DELETE FROM order_items");
                jdbcTemplate.execute("DELETE FROM orders");
                jdbcTemplate.execute("DELETE FROM cart_items");
                jdbcTemplate.execute("DELETE FROM carts");
                jdbcTemplate.execute("DELETE FROM inventory_reservations");
                jdbcTemplate.execute("DELETE FROM participations");
                jdbcTemplate.execute("DELETE FROM pricing_tiers");
                jdbcTemplate.execute("DELETE FROM campaigns");
                jdbcTemplate.execute("DELETE FROM products");
                jdbcTemplate.execute("DELETE FROM categories");
                jdbcTemplate.execute("DELETE FROM refresh_tokens");
                jdbcTemplate.execute("DELETE FROM users");
            } catch (Exception ignored) {}
        } finally {
            try {
                jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
            } catch (Exception ignored) {}
        }
    }
}
