import { apiClient } from './client';
import { Campaign, CampaignCreateRequest, CampaignDetail, CampaignUpdateRequest } from '../types/campaign';
import { Page } from '../types/common';

export const campaignApi = {
  getActiveCampaigns: async (page = 0, size = 20, sort = 'endsAt,asc'): Promise<Page<Campaign>> => {
    const { data } = await apiClient.get<Page<Campaign>>('/api/v1/campaigns', {
      params: { page, size, sort },
    });
    return data;
  },

  getCampaignDetails: async (id: number): Promise<CampaignDetail> => {
    const { data } = await apiClient.get<CampaignDetail>(`/api/v1/campaigns/${id}`);
    return data;
  },

  // Seller / Admin APIs
  getMyCampaigns: async (page = 0, size = 20): Promise<Page<Campaign>> => {
    const { data } = await apiClient.get<Page<Campaign>>('/api/v1/campaigns/seller/me', {
      params: { page, size },
    });
    return data;
  },

  createCampaign: async (request: CampaignCreateRequest): Promise<Campaign> => {
    const { data } = await apiClient.post<Campaign>('/api/v1/campaigns', request);
    return data;
  },

  updateCampaign: async (id: number, request: CampaignUpdateRequest): Promise<Campaign> => {
    const { data } = await apiClient.put<Campaign>(`/api/v1/campaigns/${id}`, request);
    return data;
  },

  cancelCampaign: async (id: number): Promise<Campaign> => {
    const { data } = await apiClient.post<Campaign>(`/api/v1/campaigns/${id}/cancel`);
    return data;
  },

  // Admin APIs
  getAllCampaignsAdmin: async (page = 0, size = 20): Promise<Page<Campaign>> => {
    const { data } = await apiClient.get<Page<Campaign>>('/api/v1/admin/campaigns', {
      params: { page, size },
    });
    return data;
  },

  finalizeCampaignAdmin: async (id: number): Promise<Campaign> => {
    const { data } = await apiClient.post<Campaign>(`/api/v1/admin/campaigns/${id}/finalize`);
    return data;
  },
};

