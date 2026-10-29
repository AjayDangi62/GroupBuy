package com.groupbuy.cart.service;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.cart.dto.CartResponse;
import com.groupbuy.cart.dto.CheckoutRequest;
import com.groupbuy.cart.entity.Cart;
import com.groupbuy.cart.entity.CartItem;
import com.groupbuy.cart.repository.CartRepository;
import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.ResourceNotFoundException;
import com.groupbuy.order.dto.OrderResponse;
import com.groupbuy.order.entity.Order;
import com.groupbuy.order.entity.OrderItem;
import com.groupbuy.order.repository.OrderRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

    public CheckoutService(CartRepository cartRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           UserRepository userRepository,
                           PricingService pricingService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, UserPrincipal principal) {
        Cart cart = cartRepository.findByUserIdWithItems(principal.getId())
                .orElseThrow(() -> new BusinessException("Shopping cart is empty", "CART_EMPTY"));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Shopping cart is empty", "CART_EMPTY");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String currency = CurrencyConstants.DEFAULT_CURRENCY;
        BigDecimal orderTotal = BigDecimal.ZERO;

        Order order = new Order(orderNumber, user, request.shippingAddress(), BigDecimal.ZERO, currency);

        // Transactional checkout with pessimistic inventory locking
        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.getProduct().getId()));

            if (product.getStatus() != Product.Status.ACTIVE) {
                throw new BusinessException("Product " + product.getName() + " is no longer active", "PRODUCT_NOT_AVAILABLE");
            }

            if (product.getAvailableInventory() < item.getQuantity()) {
                throw new BusinessException(
                        "Overselling prevented: only " + product.getAvailableInventory() + " units available for " + product.getName(),
                        "INSUFFICIENT_INVENTORY"
                );
            }

            // Deduct stock to prevent race conditions and overselling
            product.deductStock(item.getQuantity());
            productRepository.save(product);

            // Server-side price recalculation (never trust client total)
            BigDecimal unitPrice;
            Campaign campaign = item.getCampaign();
            if (campaign != null) {
                List<PricingTier> tiers = campaign.toDomainTiers();
                if (!tiers.isEmpty()) {
                    int evalUnits = Math.max(1, campaign.getCurrentPaidUnits() + item.getQuantity());
                    PricingQuoteResponse quote = pricingService.calculateQuote(evalUnits, tiers, "CHECKOUT_TIER");
                    unitPrice = quote.unitPrice();
                } else {
                    unitPrice = product.getBasePrice();
                }
            } else {
                unitPrice = product.getBasePrice();
            }

            unitPrice = unitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);

            orderTotal = orderTotal.add(itemTotal);

            // Create immutable snapshot of product details and settled unit price
            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    campaign,
                    product.getName(),
                    product.getSku(),
                    unitPrice,
                    item.getQuantity()
            );
            order.addItem(orderItem);
        }

        order.setTotalPrice(orderTotal);
        order.setStatus(Order.Status.CREATED);
        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful checkout
        cart.clear();
        cartRepository.save(cart);

        return OrderResponse.fromEntity(savedOrder);
    }
}

