import { apiClient } from './client';
import { PricingQuote, PricingTier } from '../types/campaign';

export interface PricingQuoteRequest {
  eligibleQuantity: number;
  customTiers?: PricingTier[];
}

export const pricingApi = {
  getQuote: async (request: PricingQuoteRequest): Promise<PricingQuote> => {
    const { data } = await apiClient.post<PricingQuote>('/api/v1/pricing/quote', request);
    return data;
  },
};

