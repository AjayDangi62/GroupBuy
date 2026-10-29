package com.groupbuy.pricing;

import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.InvalidPricingArgumentException;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.pricing.dto.PricingQuoteRequest;
import com.groupbuy.pricing.dto.PricingQuoteResponse;
import com.groupbuy.pricing.dto.PricingTierDto;
import com.groupbuy.pricing.service.PricingPolicyValidator;
import com.groupbuy.pricing.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PricingService Unit Tests")
class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        PricingPolicyValidator validator = new PricingPolicyValidator();
        pricingService = new PricingService(validator);
    }

    @Test
    @DisplayName("Quantity 1 selects Tier 1 (1–4 units: ₹2,000/unit)")
    void quantity1SelectsFirstTier() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(1, null));

        assertThat(quote.eligibleQuantity()).isEqualTo(1);
        assertThat(quote.unitPrice()).isEqualByComparingTo("2000.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("2000.00");
        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.tierMinQuantity()).isEqualTo(1);
        assertThat(quote.tierMaxQuantity()).isEqualTo(4);
        assertThat(quote.tierIndex()).isZero();
    }

    @Test
    @DisplayName("Quantity 4 selects Tier 1 (1–4 units: ₹2,000/unit)")
    void quantity4SelectsFirstTier() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(4, null));

        assertThat(quote.eligibleQuantity()).isEqualTo(4);
        assertThat(quote.unitPrice()).isEqualByComparingTo("2000.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("8000.00");
        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.tierMinQuantity()).isEqualTo(1);
        assertThat(quote.tierMaxQuantity()).isEqualTo(4);
        assertThat(quote.tierIndex()).isZero();
    }

    @Test
    @DisplayName("Quantity 5 selects Tier 2 (5–9 units: ₹1,800/unit)")
    void quantity5SelectsSecondTier() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(5, null));

        assertThat(quote.eligibleQuantity()).isEqualTo(5);
        assertThat(quote.unitPrice()).isEqualByComparingTo("1800.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("9000.00");
        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.tierMinQuantity()).isEqualTo(5);
        assertThat(quote.tierMaxQuantity()).isEqualTo(9);
        assertThat(quote.tierIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("Quantity 9 selects Tier 2 (5–9 units: ₹1,800/unit)")
    void quantity9SelectsSecondTier() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(9, null));

        assertThat(quote.eligibleQuantity()).isEqualTo(9);
        assertThat(quote.unitPrice()).isEqualByComparingTo("1800.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("16200.00");
        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.tierMinQuantity()).isEqualTo(5);
        assertThat(quote.tierMaxQuantity()).isEqualTo(9);
        assertThat(quote.tierIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("Quantity 10 selects Tier 3 (10–19 units: ₹1,600/unit)")
    void quantity10SelectsThirdTier() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(10, null));

        assertThat(quote.eligibleQuantity()).isEqualTo(10);
        assertThat(quote.unitPrice()).isEqualByComparingTo("1600.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("16000.00");
        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.tierMinQuantity()).isEqualTo(10);
        assertThat(quote.tierMaxQuantity()).isEqualTo(19);
        assertThat(quote.tierIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Quantity 19 selects Tier 3 (10–19 units: ₹1,600/unit)")
    void quantity19SelectsThirdTier() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(19, null));

        assertThat(quote.eligibleQuantity()).isEqualTo(19);
        assertThat(quote.unitPrice()).isEqualByComparingTo("1600.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("30400.00");
        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.tierMinQuantity()).isEqualTo(10);
        assertThat(quote.tierMaxQuantity()).isEqualTo(19);
        assertThat(quote.tierIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Quantity 20 selects highest Tier 4 (20+ units: ₹1,400/unit)")
    void quantity20SelectsHighestTier() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(20, null));

        assertThat(quote.eligibleQuantity()).isEqualTo(20);
        assertThat(quote.unitPrice()).isEqualByComparingTo("1400.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("28000.00");
        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.tierMinQuantity()).isEqualTo(20);
        assertThat(quote.tierMaxQuantity()).isNull();
        assertThat(quote.tierIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("Quantity 100 selects highest Tier 4 (open-ended 20+ units)")
    void quantity100SelectsHighestTier() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(100, null));

        assertThat(quote.eligibleQuantity()).isEqualTo(100);
        assertThat(quote.unitPrice()).isEqualByComparingTo("1400.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("140000.00");
        assertThat(quote.tierMaxQuantity()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    @DisplayName("Invalid zero or negative quantities are rejected")
    void invalidQuantitiesAreRejected(int invalidQty) {
        assertThatThrownBy(() -> pricingService.calculateQuote(new PricingQuoteRequest(invalidQty, null)))
                .isInstanceOf(InvalidPricingArgumentException.class)
                .hasMessageContaining("positive integer");
    }

    @Test
    @DisplayName("Null quantity is rejected with appropriate exception")
    void nullQuantityIsRejected() {
        assertThatThrownBy(() -> pricingService.calculateQuote(new PricingQuoteRequest(null, null)))
                .isInstanceOf(InvalidPricingArgumentException.class);
    }

    @Test
    @DisplayName("Decimal monetary calculations adhere to documented scale and HALF_EVEN rounding policy")
    void decimalCalculationsUseDocumentedRoundingPolicy() {
        // Custom policy with fractional unit price
        List<PricingTierDto> customTiers = List.of(
                new PricingTierDto(1, null, new BigDecimal("123.455"), "INR")
        );

        // Banker's rounding HALF_EVEN on 123.455 to scale 2 produces 123.46 (nearest even digit 6)
        BigDecimal expectedUnitPrice = new BigDecimal("123.455").setScale(2, RoundingMode.HALF_EVEN);
        assertThat(expectedUnitPrice).isEqualByComparingTo("123.46");

        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(3, customTiers));

        assertThat(quote.unitPrice()).isEqualByComparingTo("123.46");
        // 123.46 * 3 = 370.38
        assertThat(quote.subtotal()).isEqualByComparingTo("370.38");
        assertThat(quote.currency()).isEqualTo("INR");
        assertThat(quote.policyType()).isEqualTo("CUSTOM_CONFIGURED");
    }

    @Test
    @DisplayName("Subtotals are calculated accurately for high unit volumes")
    void subtotalsCalculatedAccurately() {
        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(15, null));

        // 15 units falls into Tier 3 (₹1600.00)
        // Subtotal = 15 * 1600.00 = 24000.00
        assertThat(quote.unitPrice()).isEqualByComparingTo("1600.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("24000.00");
        assertThat(quote.currency()).isEqualTo(CurrencyConstants.DEFAULT_CURRENCY);
    }

    @Test
    @DisplayName("Currency is handled consistently for custom currency codes")
    void customCurrencyHandledConsistently() {
        List<PricingTierDto> customTiers = List.of(
                new PricingTierDto(1, 10, new BigDecimal("50.00"), "usd"),
                new PricingTierDto(11, null, new BigDecimal("40.00"), "USD")
        );

        PricingQuoteResponse quote = pricingService.calculateQuote(new PricingQuoteRequest(15, customTiers));

        assertThat(quote.currency()).isEqualTo("USD");
        assertThat(quote.unitPrice()).isEqualByComparingTo("40.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("600.00");
    }
}
