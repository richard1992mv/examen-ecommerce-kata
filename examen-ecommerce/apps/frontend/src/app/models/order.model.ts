import { CartLineResponse } from './cart.model';

export interface OrderItemResponse extends CartLineResponse {
  id: number;
}

export interface OrderResponse {
  id: number;
  orderNumber: string;
  createdAt: string;
  couponCode: string | null;
  subtotalOriginal: number;
  categoryDiscount: number;
  volumeDiscount: number;
  couponDiscount: number;
  capAdjustment: number;
  discountTotal: number;
  effectiveDiscountPct: number;
  discountCapPct: number;
  capApplied: boolean;
  message: string | null;
  total: number;
  currency: string;
  items: OrderItemResponse[];
}
