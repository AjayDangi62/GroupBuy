package com.groupbuy.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.common.exception.GlobalExceptionHandler;
import com.groupbuy.config.CorsProperties;
import com.groupbuy.config.SecurityConfig;
import com.groupbuy.pricing.controller.PricingController;
import com.groupbuy.pricing.dto.PricingQuoteRequest;
import com.groupbuy.pricing.dto.PricingTierDto;
import com.groupbuy.pricing.service.PricingPolicyValidator;
import com.groupbuy.pricing.service.PricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PricingController.class)
@Import({SecurityConfig.class, PricingService.class, PricingPolicyValidator.class, GlobalExceptionHandler.class})
@DisplayName("PricingController API Tests")
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/pricing/quote - Valid request with default sample policy returns 200 OK")
    void validQuoteRequestWithDefaultPolicyReturns200() throws Exception {
        PricingQuoteRequest request = new PricingQuoteRequest(7, null);

        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleQuantity").value(7))
                .andExpect(jsonPath("$.unitPrice").value(1800.00))
                .andExpect(jsonPath("$.subtotal").value(12600.00))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.tierMinQuantity").value(5))
                .andExpect(jsonPath("$.tierMaxQuantity").value(9))
                .andExpect(jsonPath("$.tierIndex").value(1))
                .andExpect(jsonPath("$.policyType").value("SERVER_DEFAULT_SAMPLE"))
                .andExpect(jsonPath("$.summary").value(containsString("Tier 2")));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quote - Valid request with custom tiers returns 200 OK")
    void validQuoteRequestWithCustomTiersReturns200() throws Exception {
        List<PricingTierDto> customTiers = List.of(
                new PricingTierDto(1, 10, new BigDecimal("500.00"), "INR"),
                new PricingTierDto(11, null, new BigDecimal("400.00"), "INR")
        );
        PricingQuoteRequest request = new PricingQuoteRequest(15, customTiers);

        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleQuantity").value(15))
                .andExpect(jsonPath("$.unitPrice").value(400.00))
                .andExpect(jsonPath("$.subtotal").value(6000.00))
                .andExpect(jsonPath("$.tierMinQuantity").value(11))
                .andExpect(jsonPath("$.tierMaxQuantity").doesNotExist())
                .andExpect(jsonPath("$.policyType").value("CUSTOM_CONFIGURED"));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quote - Zero quantity fails validation with 400 Bad Request")
    void zeroQuantityFailsValidationWith400() throws Exception {
        String invalidPayload = """
                {
                    "eligibleQuantity": 0
                }
                """;

        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("eligibleQuantity"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/pricing/quote"));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quote - Negative quantity fails validation with 400 Bad Request")
    void negativeQuantityFailsValidationWith400() throws Exception {
        String invalidPayload = """
                {
                    "eligibleQuantity": -5
                }
                """;

        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("eligibleQuantity"));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quote - Malformed JSON body returns 400 Bad Request")
    void malformedJsonBodyReturns400() throws Exception {
        String malformedJson = "{ eligibleQuantity: not_a_number ";

        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/pricing/quote"));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quote - Invalid custom policy with gap returns 422 Unprocessable Entity")
    void invalidPolicyWithGapReturns422() throws Exception {
        List<PricingTierDto> tiersWithGap = List.of(
                new PricingTierDto(1, 4, new BigDecimal("100.00"), "INR"),
                new PricingTierDto(6, null, new BigDecimal("80.00"), "INR") // gap: missing 5
        );
        PricingQuoteRequest request = new PricingQuoteRequest(5, tiersWithGap);

        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errorCode").value("INVALID_PRICING_POLICY"))
                .andExpect(jsonPath("$.message").value(containsString("Gap between tiers detected")));
    }

    @Test
    @DisplayName("Error responses must never leak stack traces, database schema, or SQL")
    void errorResponseDoesNotLeakSensitiveInfo() throws Exception {
        String invalidPayload = """
                {
                    "eligibleQuantity": 0
                }
                """;

        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.sql").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
