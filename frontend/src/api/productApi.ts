import { apiClient } from './client';
import { Product, ProductCreateRequest, ProductUpdateRequest } from '../types/product';
import { Page } from '../types/common';

export interface ProductQueryParams {
  search?: string;
  categoryId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export const productApi = {
  getProducts: async (params?: ProductQueryParams): Promise<Page<Product>> => {
    const { data } = await apiClient.get<Page<Product>>('/api/v1/products', { params });
    return data;
  },

  getProductById: async (id: number): Promise<Product> => {
    const { data } = await apiClient.get<Product>(`/api/v1/products/${id}`);
    return data;
  },

  // Seller / Admin APIs
  getMyProducts: async (page = 0, size = 20): Promise<Page<Product>> => {
    const { data } = await apiClient.get<Page<Product>>('/api/v1/products/seller/me', {
      params: { page, size },
    });
    return data;
  },

  createProduct: async (request: ProductCreateRequest): Promise<Product> => {
    const { data } = await apiClient.post<Product>('/api/v1/products', request);
    return data;
  },

  updateProduct: async (id: number, request: ProductUpdateRequest): Promise<Product> => {
    const { data } = await apiClient.put<Product>(`/api/v1/products/${id}`, request);
    return data;
  },

  deleteProduct: async (id: number): Promise<{ message: string }> => {
    const { data } = await apiClient.delete<{ message: string }>(`/api/v1/products/${id}`);
    return data;
  },
};

