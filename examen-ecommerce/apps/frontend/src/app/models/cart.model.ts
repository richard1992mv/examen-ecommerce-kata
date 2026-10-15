import { ProductResponse } from './product.model';

export interface CartItemRequest {
  productId: number; // > 0
  quantity: number; // > 0
}

export interface CartRequest {
  items: CartItemRequest[]; // no vacío, productId únicos
  couponCode: string | null; // máx. 50 caracteres; null si no hay cupón
}

export interface CartLineResponse {
  productId: number;
  productName: string;
  categoryName: string;
  unitPrice: number;
  quantity: number;
  lineSubtotal: number;
  categoryDiscount: number;
  lineTotal: number;
}

export interface CartQuoteResponse {
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
  couponCode: string | null;
  items: CartLineResponse[];
}

/** Estado local del carrito (solo cliente). */
export interface CartItem {
  product: ProductResponse;
  quantity: number;
}
