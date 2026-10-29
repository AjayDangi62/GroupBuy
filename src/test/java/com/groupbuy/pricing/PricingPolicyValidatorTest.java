package com.groupbuy.pricing;

import com.groupbuy.common.exception.InvalidPricingPolicyException;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.pricing.service.PricingPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PricingPolicyValidator Unit Tests")
class PricingPolicyValidatorTest {

    private PricingPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PricingPolicyValidator();
    }

    @Test
    @DisplayName("Should accept valid 4-tier policy with open-ended final tier")
    void shouldAcceptValidPolicy() {
        List<PricingTier> validTiers = List.of(
                new PricingTier(1, 4, new BigDecimal("2000.00"), "INR"),
                new PricingTier(5, 9, new BigDecimal("1800.00"), "INR"),
                new PricingTier(10, 19, new BigDecimal("1600.00"), "INR"),
                new PricingTier(20, null, new BigDecimal("1400.00"), "INR")
        );

        assertThatCode(() -> validator.validatePolicy(validTiers))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept valid single-tier open-ended policy")
    void shouldAcceptValidSingleTierPolicy() {
        List<PricingTier> singleTier = List.of(
                new PricingTier(1, null, new BigDecimal("1000.00"), "INR")
        );

        assertThatCode(() -> validator.validatePolicy(singleTier))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject null or empty policy")
    void shouldRejectEmptyPolicy() {
        assertThatThrownBy(() -> validator.validatePolicy(null))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("at least one configured tier");

        assertThatThrownBy(() -> validator.validatePolicy(Collections.emptyList()))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("at least one configured tier");
    }

    @Test
    @DisplayName("Should reject policy where first tier does not start at quantity 1")
    void shouldRejectMissingFirstTierCoverage() {
        List<PricingTier> tiers = List.of(
                new PricingTier(2, 5, new BigDecimal("2000.00"), "INR"),
                new PricingTier(6, null, new BigDecimal("1800.00"), "INR")
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("start at quantity 1");
    }

    @Test
    @DisplayName("Should reject tier with zero or negative minQuantity")
    void shouldRejectNonPositiveMinQuantity() {
        List<PricingTier> tiers = List.of(
                new PricingTier(0, 4, new BigDecimal("2000.00"), "INR"),
                new PricingTier(5, null, new BigDecimal("1800.00"), "INR")
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class);
    }

    @Test
    @DisplayName("Should reject tier with negative unit price")
    void shouldRejectNegativeUnitPrice() {
        List<PricingTier> tiers = List.of(
                new PricingTier(1, 4, new BigDecimal("-10.00"), "INR"),
                new PricingTier(5, null, new BigDecimal("1800.00"), "INR")
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("unitPrice must be non-negative");
    }

    @Test
    @DisplayName("Should reject tier with reversed boundaries (min > max)")
    void shouldRejectReversedBoundaries() {
        List<PricingTier> tiers = List.of(
                new PricingTier(1, 10, new BigDecimal("2000.00"), "INR"),
                new PricingTier(11, 8, new BigDecimal("1800.00"), "INR"),
                new PricingTier(12, null, new BigDecimal("1600.00"), "INR")
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("reversed boundaries");
    }

    @Test
    @DisplayName("Should reject overlapping tier boundaries")
    void shouldRejectOverlappingTiers() {
        List<PricingTier> tiers = List.of(
                new PricingTier(1, 5, new BigDecimal("2000.00"), "INR"),
                new PricingTier(4, 9, new BigDecimal("1800.00"), "INR"), // starts at 4 while previous ends at 5
                new PricingTier(10, null, new BigDecimal("1600.00"), "INR")
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("Overlapping tiers detected");
    }

    @Test
    @DisplayName("Should reject gap between tiers")
    void shouldRejectGapsBetweenTiers() {
        List<PricingTier> tiers = List.of(
                new PricingTier(1, 4, new BigDecimal("2000.00"), "INR"),
                new PricingTier(6, 9, new BigDecimal("1800.00"), "INR"), // missing quantity 5
                new PricingTier(10, null, new BigDecimal("1600.00"), "INR")
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("Gap between tiers detected");
    }

    @Test
    @DisplayName("Should reject open-ended tier placed in non-terminal position")
    void shouldRejectOpenEndedTierInMiddle() {
        List<PricingTier> tiers = List.of(
                new PricingTier(1, null, new BigDecimal("2000.00"), "INR"), // open-ended in middle
                new PricingTier(5, null, new BigDecimal("1800.00"), "INR")
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("open-ended tiers are only permitted as the final tier");
    }

    @Test
    @DisplayName("Should reject policy where final tier is not open-ended")
    void shouldRejectNonOpenEndedFinalTier() {
        List<PricingTier> tiers = List.of(
                new PricingTier(1, 4, new BigDecimal("2000.00"), "INR"),
                new PricingTier(5, 10, new BigDecimal("1800.00"), "INR") // final tier is capped at 10
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("final tier (Tier 2) must be open-ended");
    }

    @Test
    @DisplayName("Should reject policy with inconsistent currencies across tiers")
    void shouldRejectInconsistentCurrencies() {
        List<PricingTier> tiers = List.of(
                new PricingTier(1, 4, new BigDecimal("2000.00"), "INR"),
                new PricingTier(5, null, new BigDecimal("25.00"), "USD") // USD mixed with INR
        );

        assertThatThrownBy(() -> validator.validatePolicy(tiers))
                .isInstanceOf(InvalidPricingPolicyException.class)
                .hasMessageContaining("does not match policy currency");
    }
}
