import { apiClient } from './client';
import { JoinCampaignRequest, Participation } from '../types/participation';
import { Page } from '../types/common';

export const participationApi = {
  joinCampaign: async (campaignId: number, request: JoinCampaignRequest): Promise<Participation> => {
    const { data } = await apiClient.post<Participation>(
      `/api/v1/campaigns/${campaignId}/participate`,
      request
    );
    return data;
  },

  cancelParticipation: async (campaignId: number): Promise<Participation> => {
    const { data } = await apiClient.delete<Participation>(
      `/api/v1/campaigns/${campaignId}/participate`
    );
    return data;
  },

  getMyParticipations: async (page = 0, size = 20): Promise<Page<Participation>> => {
    const { data } = await apiClient.get<Page<Participation>>('/api/v1/participations/me', {
      params: { page, size, sort: 'createdAt,desc' },
    });
    return data;
  },

  getCampaignParticipations: async (campaignId: number): Promise<Participation[]> => {
    const { data } = await apiClient.get<Participation[]>(
      `/api/v1/campaigns/${campaignId}/participations`
    );
    return data;
  },
};

