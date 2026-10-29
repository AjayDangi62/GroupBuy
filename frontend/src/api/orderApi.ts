import { apiClient } from './client';
import { Order, OrderStatusUpdateRequest } from '../types/order';
import { Page } from '../types/common';

export const orderApi = {
  getMyOrders: async (page = 0, size = 20): Promise<Page<Order>> => {
    const { data } = await apiClient.get<Page<Order>>('/api/v1/orders', {
      params: { page, size, sort: 'createdAt,desc' },
    });
    return data;
  },

  getOrderByNumber: async (orderNumber: string): Promise<Order> => {
    const { data } = await apiClient.get<Order>(`/api/v1/orders/${orderNumber}`);
    return data;
  },

  cancelOrder: async (orderNumber: string): Promise<Order> => {
    const { data } = await apiClient.post<Order>(`/api/v1/orders/${orderNumber}/cancel`);
    return data;
  },

  // Admin APIs
  getAllOrdersAdmin: async (page = 0, size = 20): Promise<Page<Order>> => {
    const { data } = await apiClient.get<Page<Order>>('/api/v1/admin/orders', {
      params: { page, size, sort: 'createdAt,desc' },
    });
    return data;
  },

  updateOrderStatusAdmin: async (orderId: number, status: OrderStatusUpdateRequest['status']): Promise<Order> => {
    const { data } = await apiClient.patch<Order>(`/api/v1/admin/orders/${orderId}/status`, { status });
    return data;
  },
};

