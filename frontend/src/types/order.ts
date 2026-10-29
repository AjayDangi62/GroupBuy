export interface OrderItem {
  id: number;
  productId: number;
  campaignId: number | null;
  productName: string;
  productSku: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  userId: number;
  shippingAddress: string;
  totalPrice: number;
  currency: string;
  status: 'CREATED' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderStatusUpdateRequest {
  status: 'CREATED' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
}

