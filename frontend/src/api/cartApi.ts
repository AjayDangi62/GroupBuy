import { apiClient } from './client';
import { AddToCartRequest, Cart, CheckoutRequest, UpdateCartItemRequest } from '../types/cart';
import { Order } from '../types/order';

export const cartApi = {
  getCart: async (): Promise<Cart> => {
    const { data } = await apiClient.get<Cart>('/api/v1/cart');
    return data;
  },

  addItem: async (request: AddToCartRequest): Promise<Cart> => {
    const { data } = await apiClient.post<Cart>('/api/v1/cart/items', request);
    return data;
  },

  updateQuantity: async (itemId: number, request: UpdateCartItemRequest): Promise<Cart> => {
    const { data } = await apiClient.put<Cart>(`/api/v1/cart/items/${itemId}`, request);
    return data;
  },

  removeItem: async (itemId: number): Promise<Cart> => {
    const { data } = await apiClient.delete<Cart>(`/api/v1/cart/items/${itemId}`);
    return data;
  },

  clearCart: async (): Promise<{ message: string }> => {
    const { data } = await apiClient.delete<{ message: string }>('/api/v1/cart');
    return data;
  },

  checkout: async (request: CheckoutRequest): Promise<Order> => {
    const { data } = await apiClient.post<Order>('/api/v1/cart/checkout', request);
    return data;
  },
};

