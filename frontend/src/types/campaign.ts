export interface PricingTier {
  minQuantity: number;
  maxQuantity: number | null;
  unitPrice: number;
  currency: string;
}

export interface PricingQuote {
  eligibleQuantity: number;
  unitPrice: number;
  subtotal: number;
  currency: string;
  tierMinQuantity: number;
  tierMaxQuantity: number | null;
  tierIndex: number;
  policyType: string;
  summary: string;
}

export interface Campaign {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  productBasePrice: number;
  productImageUrl?: string;
  sellerId: number;
  sellerName?: string;
  title: string;
  description?: string;
  targetMinQuantity: number;
  maxCampaignQuantity: number | null;
  currentPaidUnits: number;
  minParticipants: number;
  currentParticipants: number;
  status: 'DRAFT' | 'SCHEDULED' | 'ACTIVE' | 'FINALIZING' | 'SUCCESSFUL' | 'FAILED' | 'EXPIRED' | 'CANCELLED';
  startsAt: string;
  endsAt: string;
  finalUnitPrice: number | null;
  currency: string;
  pricingTiers: PricingTier[];
  createdAt: string;
}

export interface CampaignDetail {
  campaign: Campaign;
  currentQuote?: PricingQuote;
  quantityProgressPercent: number;
  targetMet: boolean;
}

export interface CampaignCreateRequest {
  productId: number;
  title: string;
  description?: string;
  targetMinQuantity: number;
  maxCampaignQuantity?: number | null;
  minParticipants?: number;
  startsAt: string;
  endsAt: string;
  currency?: string;
  pricingTiers: PricingTier[];
}

export interface CampaignUpdateRequest {
  title?: string;
  description?: string;
  targetMinQuantity?: number;
  maxCampaignQuantity?: number | null;
  minParticipants?: number;
  endsAt?: string;
}

