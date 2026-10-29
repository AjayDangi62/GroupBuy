import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { cartApi } from '../api/cartApi';
import { Cart } from '../types/cart';
import { useAuth } from './AuthContext';

interface CartContextType {
  cart: Cart | null;
  itemCount: number;
  isLoading: boolean;
  refreshCart: () => Promise<void>;
  addToCart: (productId: number, quantity?: number, campaignId?: number | null) => Promise<void>;
  updateQuantity: (itemId: number, quantity: number) => Promise<void>;
  removeFromCart: (itemId: number) => Promise<void>;
  clearCart: () => Promise<void>;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [cart, setCart] = useState<Cart | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const refreshCart = useCallback(async () => {
    if (!isAuthenticated) {
      setCart(null);
      return;
    }

    setIsLoading(true);
    try {
      const data = await cartApi.getCart();
      setCart(data);
    } catch {
      setCart(null);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    refreshCart();
  }, [refreshCart]);

  const addToCart = async (productId: number, quantity = 1, campaignId: number | null = null) => {
    setIsLoading(true);
    try {
      const updated = await cartApi.addItem({ productId, quantity, campaignId });
      setCart(updated);
    } finally {
      setIsLoading(false);
    }
  };

  const updateQuantity = async (itemId: number, quantity: number) => {
    setIsLoading(true);
    try {
      const updated = await cartApi.updateQuantity(itemId, { quantity });
      setCart(updated);
    } finally {
      setIsLoading(false);
    }
  };

  const removeFromCart = async (itemId: number) => {
    setIsLoading(true);
    try {
      const updated = await cartApi.removeItem(itemId);
      setCart(updated);
    } finally {
      setIsLoading(false);
    }
  };

  const clearCart = async () => {
    setIsLoading(true);
    try {
      await cartApi.clearCart();
      setCart(null);
    } finally {
      setIsLoading(false);
    }
  };

  const itemCount = cart?.items.reduce((acc, item) => acc + item.quantity, 0) || 0;

  return (
    <CartContext.Provider
      value={{
        cart,
        itemCount,
        isLoading,
        refreshCart,
        addToCart,
        updateQuantity,
        removeFromCart,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = (): CartContextType => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};

