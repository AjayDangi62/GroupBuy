import { apiClient } from './client';
import { AuthResponse, LoginRequest, RefreshTokenRequest, RegisterRequest } from '../types/auth';

export const authApi = {
  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>('/api/v1/auth/login', request);
    return data;
  },

  register: async (request: RegisterRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>('/api/v1/auth/register', request);
    return data;
  },

  refreshToken: async (request: RefreshTokenRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>('/api/v1/auth/refresh', request);
    return data;
  },

  logout: async (refreshToken?: string): Promise<void> => {
    try {
      await apiClient.post('/api/v1/auth/logout', refreshToken ? { refreshToken } : {});
    } catch {
      // Ignore network errors on logout
    }
  },
};

