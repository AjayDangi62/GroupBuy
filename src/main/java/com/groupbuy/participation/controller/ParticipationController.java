package com.groupbuy.participation.controller;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.participation.dto.JoinCampaignRequest;
import com.groupbuy.participation.dto.ParticipationResponse;
import com.groupbuy.participation.service.ParticipationService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Participations", description = "Endpoints for joining group-buying campaigns and managing buyer commitments")
@SecurityRequirement(name = "BearerAuth")
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    @PostMapping("/api/v1/campaigns/{campaignId}/participate")
    @Operation(summary = "Join group-buy campaign", description = "Reserves inventory and joins an active campaign at current authorized tier price.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successfully joined campaign"),
            @ApiResponse(responseCode = "400", description = "Campaign not active, capacity exceeded, or invalid quantity"),
            @ApiResponse(responseCode = "409", description = "User already has an active participation in this campaign")
    })
    public ResponseEntity<ParticipationResponse> joinCampaign(
            @PathVariable Long campaignId,
            @Valid @RequestBody JoinCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ParticipationResponse response = participationService.joinCampaign(campaignId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api/v1/campaigns/{campaignId}/participate")
    @Operation(summary = "Leave/Cancel participation", description = "Cancels participation before campaign deadline, releasing reserved inventory.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participation cancelled and inventory released"),
            @ApiResponse(responseCode = "400", description = "Campaign deadline has passed or already finalized"),
            @ApiResponse(responseCode = "404", description = "Active participation not found")
    })
    public ResponseEntity<ParticipationResponse> cancelParticipation(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(participationService.cancelParticipation(campaignId, principal));
    }

    @GetMapping("/api/v1/participations/me")
    @Operation(summary = "Get my campaign participations", description = "Returns all group-buying participations for the authenticated buyer.")
    public ResponseEntity<Page<ParticipationResponse>> getMyParticipations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(participationService.getMyParticipations(principal, pageable));
    }

    @GetMapping("/api/v1/campaigns/{campaignId}/participations")
    @Operation(summary = "Get campaign participations (Seller/Admin)", description = "Returns full list of participants for a given campaign. Only authorized seller or admin.")
    public ResponseEntity<List<ParticipationResponse>> getCampaignParticipations(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(participationService.getCampaignParticipations(campaignId, principal));
    }
}

