package com.groupbuy.payment.controller;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.payment.dto.PaymentInitiateRequest;
import com.groupbuy.payment.dto.PaymentResponse;
import com.groupbuy.payment.dto.PaymentVerifyRequest;
import com.groupbuy.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Endpoints for initiating, verifying, and receiving payment gateway webhooks")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment", description = "Creates a payment session with gateway and returns hosted payment URL.", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment session initiated"),
            @ApiResponse(responseCode = "400", description = "Missing order or participation target"),
            @ApiResponse(responseCode = "404", description = "Target resource not found")
    })
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentResponse response = paymentService.initiatePayment(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify payment", description = "Validates gateway response on the server side after customer completes authorization.", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment verified and state advanced"),
            @ApiResponse(responseCode = "400", description = "Verification declined or failed")
    })
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentResponse response = paymentService.verifyPayment(request, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    @Operation(summary = "Payment gateway webhook", description = "Public endpoint for asynchronous payment gateway notifications with HMAC signature verification.")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature
    ) {
        boolean processed = paymentService.processWebhook(rawPayload, signature);
        return ResponseEntity.ok(Map.of("received", true, "processed", processed));
    }
}

