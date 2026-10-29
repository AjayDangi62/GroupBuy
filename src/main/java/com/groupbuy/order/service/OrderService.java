package com.groupbuy.order.service;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.ResourceNotFoundException;
import com.groupbuy.order.dto.OrderResponse;
import com.groupbuy.order.dto.OrderStatusUpdateRequest;
import com.groupbuy.order.entity.Order;
import com.groupbuy.order.entity.OrderItem;
import com.groupbuy.order.repository.OrderItemRepository;
import com.groupbuy.order.repository.OrderRepository;
import com.groupbuy.participation.entity.Participation;
import com.groupbuy.product.entity.Product;
import com.groupbuy.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(UserPrincipal principal, Pageable pageable) {
        return orderRepository.findByUserId(principal.getId(), pageable)
                .map(OrderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(OrderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber, UserPrincipal principal) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        verifyOrderAccessOrAdmin(order, principal);
        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNumber, UserPrincipal principal) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        verifyOrderAccessOrAdmin(order, principal);

        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new BusinessException("Order is already cancelled", "ORDER_ALREADY_CANCELLED");
        }
        if (order.getStatus() == Order.Status.SHIPPED || order.getStatus() == Order.Status.DELIVERED) {
            throw new BusinessException("Cannot cancel an order that has already shipped or been delivered", "ORDER_CANNOT_BE_CANCELLED");
        }

        // Restore inventory for all items in order
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                productRepository.findByIdWithLock(item.getProduct().getId()).ifPresent(prod -> {
                    prod.restoreStock(item.getQuantity());
                    productRepository.save(prod);
                });
            }
        }

        order.setStatus(Order.Status.CANCELLED);
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Order.Status newStatus = request.status();

        // If transition is to CANCELLED, restore inventory
        if (newStatus == Order.Status.CANCELLED && order.getStatus() != Order.Status.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    productRepository.findByIdWithLock(item.getProduct().getId()).ifPresent(prod -> {
                        prod.restoreStock(item.getQuantity());
                        productRepository.save(prod);
                    });
                }
            }
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public Order createCampaignSettledOrder(Participation participation, Campaign campaign, BigDecimal finalUnitPrice, String shippingAddress) {
        String orderNumber = "GRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Product product = campaign.getProduct();

        BigDecimal lineTotal = finalUnitPrice.multiply(BigDecimal.valueOf(participation.getQuantity()))
                .setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);

        Order order = new Order(
                orderNumber,
                participation.getUser(),
                shippingAddress != null ? shippingAddress : "Standard Member Address",
                lineTotal,
                campaign.getCurrency()
        );

        order.setParticipation(participation);
        order.setCampaign(campaign);
        order.setProduct(product);
        order.setQuantity(participation.getQuantity());
        order.setFinalUnitPrice(finalUnitPrice);
        order.setStatus(Order.Status.PAID);

        OrderItem item = new OrderItem(
                order,
                product,
                campaign,
                product.getName(),
                product.getSku(),
                finalUnitPrice,
                participation.getQuantity()
        );
        order.addItem(item);

        return orderRepository.save(order);
    }

    private void verifyOrderAccessOrAdmin(Order order, UserPrincipal principal) {
        boolean isAdmin = principal != null && principal.getAuthorities() != null &&
                principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = order != null && order.getUser() != null
                && order.getUser().getId() != null
                && principal != null && principal.getId() != null
                && order.getUser().getId().equals(principal.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to view or manage this order");
        }
    }
}

