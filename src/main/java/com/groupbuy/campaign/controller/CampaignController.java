package com.groupbuy.campaign.controller;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.campaign.dto.CampaignCreateRequest;
import com.groupbuy.campaign.dto.CampaignDetailResponse;
import com.groupbuy.campaign.dto.CampaignResponse;
import com.groupbuy.campaign.dto.CampaignUpdateRequest;
import com.groupbuy.campaign.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaigns")
@Tag(name = "Campaigns", description = "Endpoints for discovering, viewing, and creating group-buying campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @Operation(summary = "Browse active campaigns", description = "Lists all currently active campaigns within their valid time window.")
    public ResponseEntity<Page<CampaignResponse>> getActiveCampaigns(
            @PageableDefault(size = 20, sort = "endsAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(campaignService.getActiveCampaigns(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign details and pricing quote", description = "Retrieves campaign information along with its evaluated live volume tier quote.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Campaign details returned"),
            @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<CampaignDetailResponse> getCampaignDetails(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaignDetails(id));
    }

    @GetMapping("/seller/me")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "List seller's campaigns", description = "Retrieves campaigns hosted by the authenticated seller.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Page<CampaignResponse>> getMyCampaigns(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(campaignService.getSellerCampaigns(principal.getId(), pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Create group-buying campaign", description = "Launches a new time-bound campaign with volume-discount pricing tiers.", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Campaign created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or date window"),
            @ApiResponse(responseCode = "422", description = "Pricing tier policy validation failed (e.g., gaps/overlaps)")
    })
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CampaignResponse response = campaignService.createCampaign(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Update campaign", description = "Updates campaign parameters before it becomes active.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CampaignUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(campaignService.updateCampaign(id, request, principal));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Cancel campaign", description = "Cancels a campaign before finalization.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CampaignResponse> cancelCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(campaignService.cancelCampaign(id, principal));
    }
}

