package com.groupbuy.cart.service;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.campaign.repository.CampaignRepository;
import com.groupbuy.cart.dto.*;
import com.groupbuy.cart.entity.Cart;
import com.groupbuy.cart.entity.CartItem;
import com.groupbuy.cart.repository.CartItemRepository;
import com.groupbuy.cart.repository.CartRepository;
import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.ResourceNotFoundException;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.pricing.dto.PricingQuoteResponse;
import com.groupbuy.pricing.service.PricingService;
import com.groupbuy.product.entity.Product;
import com.groupbuy.product.repository.ProductRepository;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       CampaignRepository campaignRepository,
                       UserRepository userRepository,
                       PricingService pricingService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
    }

    @Transactional
    public Cart getOrCreateCartEntity(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
                    Cart cart = new Cart(user);
                    return cartRepository.save(cart);
                });
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UserPrincipal principal) {
        Cart cart = getOrCreateCartEntity(principal.getId());
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addItem(AddToCartRequest request, UserPrincipal principal) {
        int quantity = request.quantity();
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero", "INVALID_QUANTITY");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (product.getStatus() != Product.Status.ACTIVE) {
            throw new BusinessException("Product is not currently available for purchase", "PRODUCT_NOT_AVAILABLE");
        }

        Campaign campaign = null;
        if (request.campaignId() != null) {
            campaign = campaignRepository.findById(request.campaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + request.campaignId()));
            if (!campaign.isJoinable()) {
                throw new BusinessException("Campaign is not currently active for purchase", "CAMPAIGN_NOT_ACTIVE");
            }
        }

        Cart cart = getOrCreateCartEntity(principal.getId());

        // Check if item already exists in cart
        Long campaignId = campaign != null ? campaign.getId() : null;
        CartItem existingItem = null;
        for (CartItem item : cart.getItems()) {
            boolean prodMatch = item.getProduct().getId().equals(product.getId());
            boolean campMatch = (campaignId == null && item.getCampaign() == null) ||
                    (campaignId != null && item.getCampaign() != null && item.getCampaign().getId().equals(campaignId));
            if (prodMatch && campMatch) {
                existingItem = item;
                break;
            }
        }

        int targetQuantity = quantity;
        if (existingItem != null) {
            targetQuantity += existingItem.getQuantity();
        }

        if (product.getAvailableInventory() < targetQuantity) {
            throw new BusinessException(
                    "Insufficient inventory: requested " + targetQuantity + " units but only " + product.getAvailableInventory() + " available",
                    "INSUFFICIENT_INVENTORY"
            );
        }

        if (existingItem != null) {
            existingItem.setQuantity(targetQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem(cart, product, campaign, quantity);
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long itemId, UpdateCartItemRequest request, UserPrincipal principal) {
        int quantity = request.quantity();
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero", "INVALID_QUANTITY");
        }

        Cart cart = getOrCreateCartEntity(principal.getId());
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item does not belong to user cart");
        }

        if (item.getProduct().getAvailableInventory() < quantity) {
            throw new BusinessException(
                    "Insufficient inventory: only " + item.getProduct().getAvailableInventory() + " available",
                    "INSUFFICIENT_INVENTORY"
            );
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long itemId, UserPrincipal principal) {
        Cart cart = getOrCreateCartEntity(principal.getId());
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item does not belong to user cart");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);

        return buildCartResponse(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        cart.clear();
        cartRepository.save(cart);
    }

    public CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        String currency = CurrencyConstants.DEFAULT_CURRENCY;

        for (CartItem item : cart.getItems()) {
            Product prod = item.getProduct();
            BigDecimal unitPrice;
            Long campaignId = null;
            String campaignTitle = null;

            if (item.getCampaign() != null) {
                Campaign camp = item.getCampaign();
                campaignId = camp.getId();
                campaignTitle = camp.getTitle();
                currency = camp.getCurrency();

                List<PricingTier> tiers = camp.toDomainTiers();
                if (!tiers.isEmpty()) {
                    int evalUnits = Math.max(1, camp.getCurrentPaidUnits() + item.getQuantity());
                    PricingQuoteResponse quote = pricingService.calculateQuote(evalUnits, tiers, "CAMPAIGN_TIER");
                    unitPrice = quote.unitPrice();
                } else {
                    unitPrice = prod.getBasePrice();
                }
            } else {
                unitPrice = prod.getBasePrice();
                currency = prod.getCurrency();
            }

            unitPrice = unitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);

            subtotal = subtotal.add(lineTotal);
            totalQuantity += item.getQuantity();

            itemResponses.add(new CartItemResponse(
                    item.getId(),
                    prod.getId(),
                    prod.getName(),
                    prod.getSku(),
                    prod.getImageUrl(),
                    campaignId,
                    campaignTitle,
                    item.getQuantity(),
                    unitPrice,
                    lineTotal,
                    currency
            ));
        }

        return new CartResponse(
                cart.getId(),
                cart.getUser().getId(),
                itemResponses,
                totalQuantity,
                subtotal.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE),
                currency
        );
    }
}

