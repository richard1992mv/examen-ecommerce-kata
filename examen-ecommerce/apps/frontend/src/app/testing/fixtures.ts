import { CartQuoteResponse, OrderResponse, ProductResponse } from '../models';

/** Datos de prueba compartidos por los specs (alineados con 02_seed_data.sql). */

export const API = {
  products: '/api/v1/products',
  quote: '/api/v1/cart/quote',
  orders: '/api/v1/orders',
  login: '/api/v1/auth/login',
} as const;

export const product = (
  id: number,
  unitPrice: number,
  stock: number,
  categoryName = 'Tecnología',
): ProductResponse => ({
  id,
  name: `Producto ${id}`,
  unitPrice,
  stock,
  categoryId: 1,
  categoryName,
});

export const quoteResponse = (overrides: Partial<CartQuoteResponse> = {}): CartQuoteResponse => ({
  subtotalOriginal: 110,
  categoryDiscount: 8,
  volumeDiscount: 5.1,
  couponDiscount: 0,
  capAdjustment: 0,
  discountTotal: 13.1,
  effectiveDiscountPct: 11.91,
  discountCapPct: 35,
  capApplied: false,
  message: null,
  total: 96.9,
  currency: 'USD',
  couponCode: null,
  items: [
    {
      productId: 1,
      productName: 'Audífonos Bluetooth',
      categoryName: 'Tecnología',
      unitPrice: 45,
      quantity: 2,
      lineSubtotal: 90,
      categoryDiscount: 9,
      lineTotal: 81,
    },
  ],
  ...overrides,
});

export const orderResponse = (overrides: Partial<OrderResponse> = {}): OrderResponse => {
  const { items, ...quote } = quoteResponse();
  return {
    ...quote,
    id: 5,
    orderNumber: 'ORD-2026-000005',
    createdAt: '2026-10-16T10:30:00',
    items: items.map((line, index) => ({ ...line, id: index + 1 })),
    ...overrides,
  };
};
