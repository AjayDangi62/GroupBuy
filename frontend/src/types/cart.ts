export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  productImageUrl?: string;
  campaignId: number | null;
  campaignTitle: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  availableStock: number;
  currency: string;
}

export interface Cart {
  id: number;
  userId: number;
  items: CartItem[];
  totalQuantity: number;
  subtotal: number;
  currency: string;
}

export interface AddToCartRequest {
  productId: number;
  campaignId?: number | null;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

export interface CheckoutRequest {
  shippingAddress: string;
  idempotencyKey?: string;
}

