package com.groupbuy.campaign.service;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.campaign.dto.CampaignCreateRequest;
import com.groupbuy.campaign.dto.CampaignDetailResponse;
import com.groupbuy.campaign.dto.CampaignResponse;
import com.groupbuy.campaign.dto.CampaignUpdateRequest;
import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.campaign.entity.CampaignPricingTier;
import com.groupbuy.campaign.repository.CampaignPricingTierRepository;
import com.groupbuy.campaign.repository.CampaignRepository;
import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.ResourceNotFoundException;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.pricing.dto.PricingQuoteResponse;
import com.groupbuy.pricing.dto.PricingTierDto;
import com.groupbuy.pricing.service.PricingPolicyValidator;
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

import java.time.Instant;
import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignPricingTierRepository tierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PricingPolicyValidator pricingPolicyValidator;
    private final PricingService pricingService;

    public CampaignService(CampaignRepository campaignRepository,
                           CampaignPricingTierRepository tierRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           PricingPolicyValidator pricingPolicyValidator,
                           PricingService pricingService) {
        this.campaignRepository = campaignRepository;
        this.tierRepository = tierRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.pricingPolicyValidator = pricingPolicyValidator;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> getActiveCampaigns(Pageable pageable) {
        return campaignRepository.findActiveCampaigns(Instant.now(), pageable)
                .map(CampaignResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> getAllCampaigns(Pageable pageable) {
        return campaignRepository.findAll(pageable)
                .map(CampaignResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> getSellerCampaigns(Long sellerId, Pageable pageable) {
        return campaignRepository.findBySellerId(sellerId, pageable)
                .map(CampaignResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public CampaignDetailResponse getCampaignDetails(Long id) {
        Campaign campaign = campaignRepository.findByIdWithTiers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        List<PricingTier> domainTiers = campaign.toDomainTiers();
        PricingQuoteResponse quote = null;
        if (!domainTiers.isEmpty()) {
            int evaluateQty = Math.max(1, campaign.getCurrentPaidUnits());
            quote = pricingService.calculateQuote(evaluateQty, domainTiers, "CAMPAIGN_CURRENT");
        }

        return CampaignDetailResponse.of(campaign, quote);
    }

    @Transactional
    public CampaignResponse createCampaign(CampaignCreateRequest request, UserPrincipal principal) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessException("Campaign end time must be strictly after start time", "INVALID_CAMPAIGN_TIME_WINDOW");
        }

        if (request.maxCampaignQuantity() != null && request.maxCampaignQuantity() < request.targetMinQuantity()) {
            throw new BusinessException("Max campaign quantity cannot be less than minimum target quantity", "INVALID_CAMPAIGN_QUANTITY");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        verifyProductOwnershipOrAdmin(product, principal);

        User seller = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        // Validate pricing tiers using existing domain validator
        List<PricingTier> domainTiers = request.pricingTiers().stream()
                .map(PricingTierDto::toDomain)
                .toList();
        pricingPolicyValidator.validatePolicy(domainTiers);

        String currency = (request.currency() != null && !request.currency().isBlank())
                ? request.currency().trim().toUpperCase()
                : product.getCurrency();

        int minParticipants = (request.minParticipants() != null && request.minParticipants() > 0)
                ? request.minParticipants()
                : 1;

        Campaign campaign = new Campaign(
                product,
                seller,
                request.title().trim(),
                request.description(),
                request.targetMinQuantity(),
                request.maxCampaignQuantity(),
                minParticipants,
                request.startsAt(),
                request.endsAt(),
                currency
        );

        for (PricingTier domainTier : domainTiers) {
            CampaignPricingTier tierEntity = new CampaignPricingTier(
                    campaign,
                    domainTier.minQuantity(),
                    domainTier.maxQuantity(),
                    domainTier.unitPrice(),
                    domainTier.currency()
            );
            campaign.addPricingTier(tierEntity);
        }

        Campaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Transactional
    public CampaignResponse updateCampaign(Long id, CampaignUpdateRequest request, UserPrincipal principal) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        verifyCampaignOwnershipOrAdmin(campaign, principal);

        if (campaign.getStatus() != Campaign.Status.DRAFT && campaign.getStatus() != Campaign.Status.SCHEDULED) {
            throw new BusinessException("Only DRAFT or SCHEDULED campaigns can be modified", "CAMPAIGN_MODIFICATION_LOCKED");
        }

        if (request.title() != null && !request.title().isBlank()) {
            campaign.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            campaign.setDescription(request.description());
        }
        if (request.targetMinQuantity() != null) {
            campaign.setTargetMinQuantity(request.targetMinQuantity());
        }
        if (request.maxCampaignQuantity() != null) {
            campaign.setMaxCampaignQuantity(request.maxCampaignQuantity());
        }
        if (request.minParticipants() != null) {
            campaign.setMinParticipants(request.minParticipants());
        }
        if (request.endsAt() != null) {
            if (!request.endsAt().isAfter(campaign.getStartsAt())) {
                throw new BusinessException("Campaign endsAt must be after startsAt", "INVALID_CAMPAIGN_TIME_WINDOW");
            }
            campaign.setEndsAt(request.endsAt());
        }

        Campaign updated = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(updated);
    }

    @Transactional
    public CampaignResponse cancelCampaign(Long id, UserPrincipal principal) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        verifyCampaignOwnershipOrAdmin(campaign, principal);

        if (campaign.getStatus() == Campaign.Status.FINALIZED || campaign.getStatus() == Campaign.Status.SUCCESSFUL) {
            throw new BusinessException("Cannot cancel an already finalized or successful campaign", "CAMPAIGN_ALREADY_FINALIZED");
        }

        campaign.setStatus(Campaign.Status.CANCELLED);
        Campaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Transactional
    public CampaignResponse finalizeCampaign(Long id) {
        Campaign campaign = campaignRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        if (campaign.getStatus() == Campaign.Status.FINALIZED || campaign.getStatus() == Campaign.Status.SUCCESSFUL) {
            return CampaignResponse.fromEntity(campaign);
        }

        campaign.setStatus(Campaign.Status.FINALIZING);

        boolean quantityMet = campaign.getCurrentPaidUnits() >= campaign.getTargetMinQuantity();
        boolean participantsMet = campaign.getCurrentParticipants() >= campaign.getMinParticipants();

        if (quantityMet && participantsMet) {
            List<PricingTier> domainTiers = campaign.toDomainTiers();
            PricingQuoteResponse quote = pricingService.calculateQuote(campaign.getCurrentPaidUnits(), domainTiers, "CAMPAIGN_FINAL");
            campaign.setFinalUnitPrice(quote.unitPrice());
            campaign.setStatus(Campaign.Status.SUCCESSFUL);
        } else {
            campaign.setStatus(Campaign.Status.FAILED);
        }

        campaign.setFinalizedAt(Instant.now());
        Campaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    private void verifyProductOwnershipOrAdmin(Product product, UserPrincipal principal) {
        boolean isAdmin = principal != null && principal.getAuthorities() != null &&
                principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = product != null && product.getSeller() != null
                && product.getSeller().getId() != null
                && principal != null && principal.getId() != null
                && product.getSeller().getId().equals(principal.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not own this product to host a campaign");
        }
    }

    private void verifyCampaignOwnershipOrAdmin(Campaign campaign, UserPrincipal principal) {
        boolean isAdmin = principal != null && principal.getAuthorities() != null &&
                principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = campaign != null && campaign.getSeller() != null
                && campaign.getSeller().getId() != null
                && principal != null && principal.getId() != null
                && campaign.getSeller().getId().equals(principal.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to manage this campaign");
        }
    }
}

