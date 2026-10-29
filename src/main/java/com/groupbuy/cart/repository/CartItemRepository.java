package com.groupbuy.cart.repository;

import com.groupbuy.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductIdAndCampaignId(Long cartId, Long productId, Long campaignId);

    Optional<CartItem> findByCartIdAndProductIdAndCampaignIsNull(Long cartId, Long productId);

    void deleteByCartId(Long cartId);
}

