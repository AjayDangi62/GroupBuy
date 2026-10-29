package com.groupbuy.campaign.service;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.campaign.dto.CampaignCreateRequest;
import com.groupbuy.campaign.dto.CampaignResponse;
import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.campaign.repository.CampaignPricingTierRepository;
import com.groupbuy.campaign.repository.CampaignRepository;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.pricing.dto.PricingTierDto;
import com.groupbuy.pricing.service.PricingPolicyValidator;
import com.groupbuy.pricing.service.PricingService;
import com.groupbuy.product.entity.Product;
import com.groupbuy.product.repository.ProductRepository;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignService Unit Tests")
class CampaignServiceTest {

    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignPricingTierRepository tierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private PricingPolicyValidator pricingPolicyValidator;
    @Mock private PricingService pricingService;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(
                campaignRepository, tierRepository, productRepository,
                userRepository, pricingPolicyValidator, pricingService
        );
    }

    @Test
    @DisplayName("createCampaign rejects reversed dates where endsAt is before startsAt")
    void createCampaignRejectsReversedDates() {
        Instant now = Instant.now();
        CampaignCreateRequest request = new CampaignCreateRequest(
                1L, "Title", "Desc", 10, 50, 2,
                now.plus(5, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS), // Ends before start
                "INR",
                List.of(new PricingTierDto(1, null, new BigDecimal("100.00"), "INR"))
        );

        UserPrincipal principal = new UserPrincipal(1L, "seller@test.com", "seller", "pass", "Seller", List.of(), true);

        assertThatThrownBy(() -> campaignService.createCampaign(request, principal))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("end time must be strictly after start time");
    }

    @Test
    @DisplayName("createCampaign rejects non-owning seller when trying to host campaign for another's product")
    void createCampaignRejectsNonOwner() {
        Instant now = Instant.now();
        CampaignCreateRequest request = new CampaignCreateRequest(
                1L, "Title", "Desc", 10, 50, 2,
                now, now.plus(5, ChronoUnit.DAYS), "INR",
                List.of(new PricingTierDto(1, null, new BigDecimal("100.00"), "INR"))
        );

        User owner = new User("owner@test.com", "owner", "pass", "Owner", User.Role.SELLER);
        ReflectionTestUtils.setField(owner, "id", 99L);
        Product product = new Product(owner, null, "SKU-1", "Product", "Desc", new BigDecimal("100.00"), "INR", 10, 10, Product.Status.ACTIVE, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Caller has user ID 10L, different from owner ID 99L
        UserPrincipal principal = new UserPrincipal(10L, "hacker@test.com", "hacker", "pass", "Hacker", List.of(), true);

        assertThatThrownBy(() -> campaignService.createCampaign(request, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this product to host a campaign");
    }

    @Test
    @DisplayName("createCampaign rejects when product seller id is null")
    void createCampaignRejectsNullSellerId() {
        Instant now = Instant.now();
        CampaignCreateRequest request = new CampaignCreateRequest(
                1L, "Title", "Desc", 10, 50, 2,
                now, now.plus(5, ChronoUnit.DAYS), "INR",
                List.of(new PricingTierDto(1, null, new BigDecimal("100.00"), "INR"))
        );

        User owner = new User("owner@test.com", "owner", "pass", "Owner", User.Role.SELLER);
        // owner.getId() is intentionally null
        Product product = new Product(owner, null, "SKU-1", "Product", "Desc", new BigDecimal("100.00"), "INR", 10, 10, Product.Status.ACTIVE, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        UserPrincipal principal = new UserPrincipal(10L, "seller@test.com", "seller", "pass", "Seller", List.of(), true);

        assertThatThrownBy(() -> campaignService.createCampaign(request, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this product to host a campaign");
    }

    @Test
    @DisplayName("createCampaign rejects when principal id is null")
    void createCampaignRejectsNullPrincipalId() {
        Instant now = Instant.now();
        CampaignCreateRequest request = new CampaignCreateRequest(
                1L, "Title", "Desc", 10, 50, 2,
                now, now.plus(5, ChronoUnit.DAYS), "INR",
                List.of(new PricingTierDto(1, null, new BigDecimal("100.00"), "INR"))
        );

        User owner = new User("owner@test.com", "owner", "pass", "Owner", User.Role.SELLER);
        ReflectionTestUtils.setField(owner, "id", 99L);
        Product product = new Product(owner, null, "SKU-1", "Product", "Desc", new BigDecimal("100.00"), "INR", 10, 10, Product.Status.ACTIVE, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        UserPrincipal principal = new UserPrincipal(null, "seller@test.com", "seller", "pass", "Seller", List.of(), true);

        assertThatThrownBy(() -> campaignService.createCampaign(request, principal))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("createCampaign allows admin user even if admin is not the product seller")
    void createCampaignAllowsAdmin() {
        Instant now = Instant.now();
        CampaignCreateRequest request = new CampaignCreateRequest(
                1L, "Title", "Desc", 10, 50, 2,
                now, now.plus(5, ChronoUnit.DAYS), "INR",
                List.of(new PricingTierDto(1, null, new BigDecimal("100.00"), "INR"))
        );

        User owner = new User("owner@test.com", "owner", "pass", "Owner", User.Role.SELLER);
        ReflectionTestUtils.setField(owner, "id", 99L);
        Product product = new Product(owner, null, "SKU-1", "Product", "Desc", new BigDecimal("100.00"), "INR", 10, 10, Product.Status.ACTIVE, null);

        User adminUser = new User("admin@test.com", "admin", "pass", "Admin", User.Role.ADMIN);
        ReflectionTestUtils.setField(adminUser, "id", 1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SimpleGrantedAuthority adminAuth = new SimpleGrantedAuthority("ROLE_ADMIN");
        UserPrincipal adminPrincipal = new UserPrincipal(1L, "admin@test.com", "admin", "pass", "Admin", List.of(adminAuth), true);

        CampaignResponse response = campaignService.createCampaign(request, adminPrincipal);
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Title");
    }
}
