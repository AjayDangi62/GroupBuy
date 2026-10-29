import { apiClient } from './client';
import { UserProfile } from '../types/user';

export const userApi = {
  getCurrentUser: async (): Promise<UserProfile> => {
    const { data } = await apiClient.get<UserProfile>('/api/v1/users/me');
    return data;
  },
};

