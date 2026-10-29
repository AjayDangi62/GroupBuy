package com.groupbuy.campaign.controller;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.campaign.dto.CampaignResponse;
import com.groupbuy.campaign.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Operations - Campaigns", description = "Administrative endpoints for campaign oversight and manual finalization")
@SecurityRequirement(name = "BearerAuth")
public class AdminCampaignController {

    private final CampaignService campaignService;

    public AdminCampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @Operation(summary = "List all campaigns (Admin)", description = "Retrieves all campaigns across all statuses and sellers.")
    public ResponseEntity<Page<CampaignResponse>> getAllCampaigns(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(campaignService.getAllCampaigns(pageable));
    }

    @PostMapping("/{id}/finalize")
    @Operation(summary = "Manually finalize campaign (Admin)", description = "Forces evaluation and finalization of campaign volume pricing.")
    public ResponseEntity<CampaignResponse> finalizeCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.finalizeCampaign(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Force cancel campaign (Admin)", description = "Administratively cancels a campaign.")
    public ResponseEntity<CampaignResponse> cancelCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(campaignService.cancelCampaign(id, principal));
    }
}

