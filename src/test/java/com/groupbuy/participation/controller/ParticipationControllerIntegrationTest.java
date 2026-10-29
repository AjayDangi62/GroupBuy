package com.groupbuy.participation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.campaign.entity.CampaignPricingTier;
import com.groupbuy.campaign.repository.CampaignRepository;
import com.groupbuy.category.entity.Category;
import com.groupbuy.category.repository.CategoryRepository;
import com.groupbuy.participation.dto.JoinCampaignRequest;
import com.groupbuy.participation.repository.InventoryReservationRepository;
import com.groupbuy.participation.repository.ParticipationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Participation Controller Integration Tests")
class ParticipationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.groupbuy.TestDatabaseCleaner databaseCleaner;

    @Autowired
    private ParticipationRepository participationRepository;

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

    private String buyerToken;
    private String sellerToken;
    private User buyer;
    private User seller;
    private Campaign campaign;
    private Product product;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        seller = new User("seller@groupbuy.com", "seller_part", passwordEncoder.encode("Pass123!"), "Seller", User.Role.SELLER);
        seller = userRepository.save(seller);
        sellerToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(seller));

        buyer = new User("buyer@groupbuy.com", "buyer_part", passwordEncoder.encode("Pass123!"), "Buyer", User.Role.CUSTOMER);
        buyer = userRepository.save(buyer);
        buyerToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(buyer));

        Category cat = new Category("Electronics", "electronics", "Desc", null, true, 1);
        cat = categoryRepository.save(cat);

        product = new Product(seller, cat, "SKU-PART", "Wireless Earphones", "Audio", new BigDecimal("2000.00"), "INR", 50, 50, Product.Status.ACTIVE, null);
        product = productRepository.save(product);

        campaign = new Campaign(
                product,
                seller,
                "Earphones Group Buy",
                "Save on bulk",
                10,
                50,
                2,
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(5, ChronoUnit.DAYS),
                "INR"
        );
        campaign.setStatus(Campaign.Status.ACTIVE);
        campaign.addPricingTier(new CampaignPricingTier(campaign, 1, 4, new BigDecimal("2000.00"), "INR"));
        campaign.addPricingTier(new CampaignPricingTier(campaign, 5, 9, new BigDecimal("1800.00"), "INR"));
        campaign.addPricingTier(new CampaignPricingTier(campaign, 10, null, new BigDecimal("1500.00"), "INR"));
        campaign = campaignRepository.save(campaign);
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/participate - Buyer successfully joins campaign, reserves inventory and locks price")
    void buyerCanJoinCampaign() throws Exception {
        JoinCampaignRequest request = new JoinCampaignRequest(2);

        mockMvc.perform(post("/api/v1/campaigns/" + campaign.getId() + "/participate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.authorizedUnitPrice").value(2000.00))
                .andExpect(jsonPath("$.authorizedTotalAmount").value(4000.00))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Verify inventory was deducted from available product stock
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getAvailableInventory()).isEqualTo(48);

        // Verify campaign counts incremented
        Campaign updatedCampaign = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(updatedCampaign.getCurrentPaidUnits()).isEqualTo(2);
        assertThat(updatedCampaign.getCurrentParticipants()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/participate - Rejects duplicate participation in same campaign with 409 Conflict")
    void duplicateParticipationPrevented() throws Exception {
        JoinCampaignRequest request = new JoinCampaignRequest(1);

        mockMvc.perform(post("/api/v1/campaigns/" + campaign.getId() + "/participate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/campaigns/" + campaign.getId() + "/participate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_PARTICIPATION"));
    }

    @Test
    @DisplayName("DELETE /api/v1/campaigns/{id}/participate - Buyer can leave campaign and inventory is restored")
    void buyerCanLeaveCampaign() throws Exception {
        JoinCampaignRequest request = new JoinCampaignRequest(3);

        mockMvc.perform(post("/api/v1/campaigns/" + campaign.getId() + "/participate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/campaigns/" + campaign.getId() + "/participate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Product restoredProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(restoredProduct.getAvailableInventory()).isEqualTo(50);
    }
}

