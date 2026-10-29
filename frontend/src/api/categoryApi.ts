import { apiClient } from './client';
import { Category, CategoryRequest } from '../types/category';

export const categoryApi = {
  getActiveCategories: async (): Promise<Category[]> => {
    const { data } = await apiClient.get<Category[]>('/api/v1/categories');
    return data;
  },

  getCategoryById: async (id: number): Promise<Category> => {
    const { data } = await apiClient.get<Category>(`/api/v1/categories/${id}`);
    return data;
  },

  getCategoryBySlug: async (slug: string): Promise<Category> => {
    const { data } = await apiClient.get<Category>(`/api/v1/categories/slug/${slug}`);
    return data;
  },

  // Admin APIs
  getAllCategoriesAdmin: async (): Promise<Category[]> => {
    const { data } = await apiClient.get<Category[]>('/api/v1/admin/categories');
    return data;
  },

  createCategory: async (request: CategoryRequest): Promise<Category> => {
    const { data } = await apiClient.post<Category>('/api/v1/admin/categories', request);
    return data;
  },

  updateCategory: async (id: number, request: CategoryRequest): Promise<Category> => {
    const { data } = await apiClient.put<Category>(`/api/v1/admin/categories/${id}`, request);
    return data;
  },

  deleteCategory: async (id: number): Promise<{ message: string }> => {
    const { data } = await apiClient.delete<{ message: string }>(`/api/v1/admin/categories/${id}`);
    return data;
  },
};

