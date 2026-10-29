package com.groupbuy.pricing.controller;

import com.groupbuy.common.exception.ErrorResponse;
import com.groupbuy.pricing.dto.PricingQuoteRequest;
import com.groupbuy.pricing.dto.PricingQuoteResponse;
import com.groupbuy.pricing.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing pricing tier evaluation and quote calculation.
 */
@RestController
@RequestMapping("/api/v1/pricing")
@Tag(name = "Pricing", description = "Dynamic tiered pricing and volume discount evaluation")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/quote")
    @Operation(
            summary = "Calculate tiered pricing quote",
            description = "Computes the unit price, qualifying tier boundaries, and subtotal for a given quantity of eligible units. " +
                          "If customTiers is omitted, the documented server-managed sample policy (Wireless Headphones) is used."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pricing quote calculated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PricingQuoteResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or quantity argument",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Custom pricing policy validation failed (e.g. gaps, overlaps, inverted boundaries)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<PricingQuoteResponse> calculateQuote(@Valid @RequestBody PricingQuoteRequest request) {
        PricingQuoteResponse quote = pricingService.calculateQuote(request);
        return ResponseEntity.ok(quote);
    }
}
