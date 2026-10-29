package com.groupbuy.campaign.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.campaign.dto.CampaignCreateRequest;
import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.campaign.repository.CampaignRepository;
import com.groupbuy.category.entity.Category;
import com.groupbuy.category.repository.CategoryRepository;
import com.groupbuy.pricing.dto.PricingTierDto;
import com.groupbuy.product.entity.Product;
import com.groupbuy.product.repository.ProductRepository;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Campaign Controller Integration Tests")
class CampaignControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.groupbuy.TestDatabaseCleaner databaseCleaner;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.groupbuy.auth.security.JwtTokenProvider jwtTokenProvider;

    private String sellerToken;
    private String adminToken;
    private User seller;
    private Product product;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        seller = new User("seller@groupbuy.com", "seller_campaign", passwordEncoder.encode("Pass123!"), "Campaign Seller", User.Role.SELLER);
        seller = userRepository.save(seller);
        sellerToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(seller));

        User admin = new User("admin@groupbuy.com", "admin_campaign", passwordEncoder.encode("Pass123!"), "Admin", User.Role.ADMIN);
        admin = userRepository.save(admin);
        adminToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(admin));

        Category cat = new Category("Electronics", "electronics", "Desc", null, true, 1);
        cat = categoryRepository.save(cat);

        product = new Product(seller, cat, "SKU-CAMP-1", "Flagship Phone", "Smartphone", new BigDecimal("50000.00"), "INR", 100, 100, Product.Status.ACTIVE, null);
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("POST /api/v1/campaigns - Seller can create group-buy campaign with validated pricing tiers")
    void sellerCanCreateCampaign() throws Exception {
        Instant startsAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endsAt = Instant.now().plus(7, ChronoUnit.DAYS);

        List<PricingTierDto> tiers = List.of(
                new PricingTierDto(1, 4, new BigDecimal("48000.00"), "INR"),
                new PricingTierDto(5, 9, new BigDecimal("45000.00"), "INR"),
                new PricingTierDto(10, null, new BigDecimal("42000.00"), "INR")
        );

        CampaignCreateRequest request = new CampaignCreateRequest(
                product.getId(),
                "Flagship Phone Mega Group Buy",
                "Buy together and save up to 8000 INR",
                10,
                50,
                3,
                startsAt,
                endsAt,
                "INR",
                tiers
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Flagship Phone Mega Group Buy"))
                .andExpect(jsonPath("$.targetMinQuantity").value(10))
                .andExpect(jsonPath("$.minParticipants").value(3))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.pricingTiers.length()").value(3));

        assertThat(campaignRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/campaigns - Rejects pricing policy with gaps via 422 Unprocessable Entity")
    void rejectsInvalidPricingPolicyWithGaps() throws Exception {
        Instant startsAt = Instant.now();
        Instant endsAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // Gap: 1-4 then 6+ (missing 5)
        List<PricingTierDto> brokenTiers = List.of(
                new PricingTierDto(1, 4, new BigDecimal("48000.00"), "INR"),
                new PricingTierDto(6, null, new BigDecimal("42000.00"), "INR")
        );

        CampaignCreateRequest request = new CampaignCreateRequest(
                product.getId(),
                "Broken Campaign",
                "Has gap",
                10,
                null,
                1,
                startsAt,
                endsAt,
                "INR",
                brokenTiers
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PRICING_POLICY"));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns/{id} - Public details endpoint calculates live pricing quote")
    void publicCanViewCampaignDetailsWithLiveQuote() throws Exception {
        Instant startsAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endsAt = Instant.now().plus(7, ChronoUnit.DAYS);

        List<PricingTierDto> tiers = List.of(
                new PricingTierDto(1, 4, new BigDecimal("48000.00"), "INR"),
                new PricingTierDto(5, null, new BigDecimal("45000.00"), "INR")
        );

        CampaignCreateRequest request = new CampaignCreateRequest(
                product.getId(),
                "Live Quote Test",
                "Details",
                5,
                null,
                1,
                startsAt,
                endsAt,
                "INR",
                tiers
        );

        String json = mockMvc.perform(post("/api/v1/campaigns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long campaignId = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(get("/api/v1/campaigns/" + campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.title").value("Live Quote Test"))
                .andExpect(jsonPath("$.currentQuote.unitPrice").value(48000.00))
                .andExpect(jsonPath("$.targetMet").value(false));
    }
}

