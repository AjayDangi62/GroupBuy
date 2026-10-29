package com.groupbuy.participation.service;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.campaign.repository.CampaignRepository;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.DuplicateResourceException;
import com.groupbuy.common.exception.ResourceNotFoundException;
import com.groupbuy.participation.dto.JoinCampaignRequest;
import com.groupbuy.participation.dto.ParticipationResponse;
import com.groupbuy.participation.entity.InventoryReservation;
import com.groupbuy.participation.entity.Participation;
import com.groupbuy.participation.repository.InventoryReservationRepository;
import com.groupbuy.participation.repository.ParticipationRepository;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.pricing.dto.PricingQuoteResponse;
import com.groupbuy.pricing.service.PricingService;
import com.groupbuy.product.entity.Product;
import com.groupbuy.product.repository.ProductRepository;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ParticipationService {

    private static final int RESERVATION_EXPIRY_MINUTES = 15;

    private final ParticipationRepository participationRepository;
    private final InventoryReservationRepository reservationRepository;
    private final CampaignRepository campaignRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

    public ParticipationService(ParticipationRepository participationRepository,
                                InventoryReservationRepository reservationRepository,
                                CampaignRepository campaignRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository,
                                PricingService pricingService) {
        this.participationRepository = participationRepository;
        this.reservationRepository = reservationRepository;
        this.campaignRepository = campaignRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
    }

    @Transactional
    public ParticipationResponse joinCampaign(Long campaignId, JoinCampaignRequest request, UserPrincipal principal) {
        int quantity = request.quantity();
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero", "INVALID_QUANTITY");
        }

        // Prevent sellers from participating in their own campaigns
        Campaign campaign = campaignRepository.findByIdWithLock(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        if (campaign.getSeller() != null && campaign.getSeller().getId().equals(principal.getId())) {
            throw new BusinessException("Sellers cannot join their own campaign", "SELLER_JOIN_FORBIDDEN");
        }

        if (!campaign.isJoinable()) {
            throw new BusinessException("Campaign is not active or has reached its capacity/deadline", "CAMPAIGN_NOT_JOINABLE");
        }

        // Prevent duplicate active participation
        participationRepository.findActiveByCampaignAndUser(campaignId, principal.getId()).ifPresent(p -> {
            throw new DuplicateResourceException(
                    "You already have an active participation in this campaign (ID: " + p.getId() + ")",
                    "DUPLICATE_PARTICIPATION"
            );
        });

        // Concurrency-safe inventory hold
        Product product = productRepository.findByIdWithLock(campaign.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getAvailableInventory() < quantity) {
            throw new BusinessException(
                    "Insufficient product inventory available: requested " + quantity + " but only " + product.getAvailableInventory() + " available",
                    "INSUFFICIENT_INVENTORY"
            );
        }

        product.deductStock(quantity);
        productRepository.save(product);

        // Calculate authorized unit price from current campaign tier curve
        List<PricingTier> domainTiers = campaign.toDomainTiers();
        int simulatedUnits = campaign.getCurrentPaidUnits() + quantity;
        PricingQuoteResponse quote = pricingService.calculateQuote(simulatedUnits, domainTiers, "CAMPAIGN_TIER");
        BigDecimal authorizedUnitPrice = quote.unitPrice();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        Participation participation = new Participation(campaign, user, quantity, authorizedUnitPrice);
        participation.setStatus(Participation.Status.CONFIRMED);
        Participation savedParticipation = participationRepository.save(participation);

        // Update campaign aggregated counts atomically
        campaign.incrementParticipation(quantity);
        campaignRepository.save(campaign);

        // Record inventory reservation
        Instant expiresAt = Instant.now().plus(RESERVATION_EXPIRY_MINUTES, ChronoUnit.MINUTES);
        InventoryReservation reservation = new InventoryReservation(campaign, product, savedParticipation, quantity, expiresAt);
        reservation.setStatus(InventoryReservation.Status.COMMITTED);
        reservationRepository.save(reservation);

        return ParticipationResponse.fromEntity(savedParticipation);
    }

    @Transactional
    public ParticipationResponse cancelParticipation(Long campaignId, UserPrincipal principal) {
        Campaign campaign = campaignRepository.findByIdWithLock(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        if (campaign.getStatus() != Campaign.Status.ACTIVE) {
            throw new BusinessException("Cannot leave a campaign that is no longer active", "CAMPAIGN_NOT_ACTIVE");
        }

        Participation participation = participationRepository.findActiveByCampaignAndUser(campaignId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No active participation found in this campaign for user"));

        // Restore inventory
        Product product = productRepository.findByIdWithLock(campaign.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.restoreStock(participation.getQuantity());
        productRepository.save(product);

        // Update campaign numbers
        campaign.decrementParticipation(participation.getQuantity());
        campaignRepository.save(campaign);

        // Update reservation
        reservationRepository.findByParticipationId(participation.getId()).ifPresent(r -> {
            r.setStatus(InventoryReservation.Status.RELEASED);
            reservationRepository.save(r);
        });

        participation.setStatus(Participation.Status.CANCELLED);
        Participation saved = participationRepository.save(participation);
        return ParticipationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<ParticipationResponse> getMyParticipations(UserPrincipal principal, Pageable pageable) {
        return participationRepository.findByUserId(principal.getId(), pageable)
                .map(ParticipationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ParticipationResponse> getCampaignParticipations(Long campaignId, UserPrincipal principal) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        boolean isAdmin = principal != null && principal.getAuthorities() != null &&
                principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = campaign != null && campaign.getSeller() != null
                && campaign.getSeller().getId() != null
                && principal != null && principal.getId() != null
                && campaign.getSeller().getId().equals(principal.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Only the seller or an admin may view full campaign participant details");
        }

        return participationRepository.findByCampaignId(campaignId).stream()
                .map(ParticipationResponse::fromEntity)
                .toList();
    }
}

