export interface Participation {
  id: number;
  campaignId: number;
  campaignTitle: string;
  productId: number;
  productName: string;
  userId: number;
  username: string;
  quantity: number;
  authorizedUnitPrice: number;
  authorizedTotalAmount: number;
  finalUnitPrice: number | null;
  finalTotalAmount: number | null;
  currency: string;
  status: 'PENDING' | 'CONFIRMED' | 'SETTLED' | 'CANCELLED';
  joinedAt: string;
}

export interface JoinCampaignRequest {
  quantity: number;
}

