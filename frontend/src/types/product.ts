export interface Product {
  id: number;
  sellerId: number;
  sellerName?: string;
  categoryId?: number;
  categoryName?: string;
  sku: string;
  name: string;
  description?: string;
  basePrice: number;
  currency: string;
  totalInventory: number;
  availableInventory: number;
  status: 'ACTIVE' | 'ARCHIVED' | 'OUT_OF_STOCK';
  imageUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductCreateRequest {
  categoryId?: number;
  sku: string;
  name: string;
  description?: string;
  basePrice: number;
  currency?: string;
  totalInventory: number;
  availableInventory: number;
  imageUrl?: string;
}

export interface ProductUpdateRequest {
  categoryId?: number;
  name?: string;
  description?: string;
  basePrice?: number;
  totalInventory?: number;
  availableInventory?: number;
  status?: 'ACTIVE' | 'ARCHIVED' | 'OUT_OF_STOCK';
  imageUrl?: string;
}

