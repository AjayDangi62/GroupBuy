package com.groupbuy.campaign.service;

import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.campaign.repository.CampaignRepository;
import com.groupbuy.order.service.OrderService;
import com.groupbuy.participation.entity.InventoryReservation;
import com.groupbuy.participation.entity.Participation;
import com.groupbuy.participation.repository.InventoryReservationRepository;
import com.groupbuy.participation.repository.ParticipationRepository;
import com.groupbuy.payment.entity.Payment;
import com.groupbuy.payment.repository.PaymentRepository;
import com.groupbuy.payment.service.PaymentService;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.pricing.dto.PricingQuoteResponse;
import com.groupbuy.pricing.service.PricingService;
import com.groupbuy.product.entity.Product;
import com.groupbuy.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class CampaignScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignScheduler.class);

    private final CampaignRepository campaignRepository;
    private final ParticipationRepository participationRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PricingService pricingService;

    public CampaignScheduler(CampaignRepository campaignRepository,
                             ParticipationRepository participationRepository,
                             InventoryReservationRepository reservationRepository,
                             ProductRepository productRepository,
                             PaymentRepository paymentRepository,
                             PaymentService paymentService,
                             OrderService orderService,
                             PricingService pricingService) {
        this.campaignRepository = campaignRepository;
        this.participationRepository = participationRepository;
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.pricingService = pricingService;
    }

    /**
     * Periodically activates SCHEDULED campaigns whose start time has arrived.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void activateScheduledCampaigns() {
        List<Campaign> ready = campaignRepository.findScheduledCampaignsReadyToStart(Instant.now());
        for (Campaign campaign : ready) {
            log.info("Activating scheduled campaign ID: {}, title: {}", campaign.getId(), campaign.getTitle());
            campaign.setStatus(Campaign.Status.ACTIVE);
            campaignRepository.save(campaign);
        }
    }

    /**
     * Periodically finalizes ACTIVE campaigns whose deadline has elapsed.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processDueCampaigns() {
        List<Campaign> dueCampaigns = campaignRepository.findActiveCampaignsDueForFinalization(Instant.now());
        for (Campaign campaign : dueCampaigns) {
            try {
                settleCampaign(campaign);
            } catch (Exception ex) {
                log.error("Failed to automatically finalize campaign ID: {}", campaign.getId(), ex);
            }
        }
    }

    /**
     * Periodically cleans up expired pending inventory holds.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void releaseExpiredReservations() {
        List<InventoryReservation> expired = reservationRepository.findExpiredPendingReservations(Instant.now());
        for (InventoryReservation res : expired) {
            log.info("Releasing expired inventory reservation ID: {}, qty: {}", res.getId(), res.getReservedQuantity());
            res.setStatus(InventoryReservation.Status.EXPIRED);
            reservationRepository.save(res);

            productRepository.findByIdWithLock(res.getProduct().getId()).ifPresent(p -> {
                p.restoreStock(res.getReservedQuantity());
                productRepository.save(p);
            });
        }
    }

    private void settleCampaign(Campaign campaign) {
        log.info("Beginning settlement for campaign ID: {}, units: {}, participants: {}",
                campaign.getId(), campaign.getCurrentPaidUnits(), campaign.getCurrentParticipants());

        campaign.setStatus(Campaign.Status.FINALIZING);

        boolean quantityMet = campaign.getCurrentPaidUnits() >= campaign.getTargetMinQuantity();
        boolean participantsMet = campaign.getCurrentParticipants() >= campaign.getMinParticipants();

        if (quantityMet && participantsMet) {
            List<PricingTier> domainTiers = campaign.toDomainTiers();
            PricingQuoteResponse quote = pricingService.calculateQuote(campaign.getCurrentPaidUnits(), domainTiers, "CAMPAIGN_FINAL");

            campaign.setFinalUnitPrice(quote.unitPrice());
            campaign.setStatus(Campaign.Status.SUCCESSFUL);
            campaign.setFinalizedAt(Instant.now());
            campaignRepository.save(campaign);

            // Settle all active participations, capture payments, generate immutable orders
            List<Participation> participations = participationRepository.findByCampaignId(campaign.getId());
            for (Participation p : participations) {
                if (p.getStatus() == Participation.Status.CONFIRMED || p.getStatus() == Participation.Status.AUTHORIZED) {
                    p.settleFinalPrice(quote.unitPrice());
                    participationRepository.save(p);

                    // Settle payment differential
                    Optional<Payment> paymentOpt = paymentRepository.findByParticipationId(p.getId());
                    paymentOpt.ifPresent(pay -> paymentService.captureAndRefundDifferential(pay, quote.unitPrice(), p.getQuantity()));

                    // Snapshot settled order
                    orderService.createCampaignSettledOrder(p, campaign, quote.unitPrice(), "Standard Member Address");
                }
            }
            log.info("Successfully settled campaign ID: {} at unit price: {}", campaign.getId(), quote.unitPrice());
        } else {
            campaign.setStatus(Campaign.Status.FAILED);
            campaign.setFinalizedAt(Instant.now());
            campaignRepository.save(campaign);

            // Void reservations and restore stock
            List<Participation> participations = participationRepository.findByCampaignId(campaign.getId());
            for (Participation p : participations) {
                if (p.getStatus() == Participation.Status.CONFIRMED || p.getStatus() == Participation.Status.AUTHORIZED) {
                    p.setStatus(Participation.Status.CANCELLED);
                    participationRepository.save(p);

                    productRepository.findByIdWithLock(campaign.getProduct().getId()).ifPresent(prod -> {
                        prod.restoreStock(p.getQuantity());
                        productRepository.save(prod);
                    });
                }
            }
            log.warn("Campaign ID: {} failed to reach targets (target: {}, actual: {}). Released holds.",
                    campaign.getId(), campaign.getTargetMinQuantity(), campaign.getCurrentPaidUnits());
        }
    }
}

