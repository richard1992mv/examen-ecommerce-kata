import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CartQuoteResponse, ProductResponse } from '../models';
import { CartService, REQUOTE_DEBOUNCE_MS } from './cart.service';

const product = (id: number, unitPrice: number, stock: number, categoryName = 'Tecnología'): ProductResponse => ({
  id,
  name: `Producto ${id}`,
  unitPrice,
  stock,
  categoryId: 1,
  categoryName,
});

const AUDIFONOS = product(1, 45, 25);
const MOUSE = product(2, 20, 40);
const SARTENES = product(7, 55, 2, 'Hogar');
const QUOTE_URL = '/api/v1/cart/quote';

const quoteResponse = (overrides: Partial<CartQuoteResponse> = {}): CartQuoteResponse => ({
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
  items: [],
  ...overrides,
});

describe('CartService', () => {
  let service: CartService;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('inicia vacío con subtotal 0', () => {
    expect(service.isEmpty()).toBe(true);
    expect(service.subtotalOriginal()).toBe(0);
  });

  it('agregar un producto existente incrementa la cantidad (sin productId repetidos)', () => {
    service.add(AUDIFONOS);
    service.add(AUDIFONOS);
    expect(service.items().length).toBe(1);
    expect(service.quantityOf(1)).toBe(2);
  });

  it('calcula el subtotal original Σ unitPrice × quantity en tiempo real', () => {
    service.add(AUDIFONOS); // 45
    service.add(MOUSE);
    service.increment(2); // 2 × 20
    expect(service.subtotalOriginal()).toBe(85);
    service.decrement(2);
    expect(service.subtotalOriginal()).toBe(65);
  });

  it('no permite superar el stock', () => {
    service.setQuantity(SARTENES, 5);
    expect(service.quantityOf(7)).toBe(2);
    service.add(SARTENES);
    expect(service.quantityOf(7)).toBe(2);
  });

  it('no agrega productos agotados', () => {
    service.add(product(5, 12.5, 0));
    expect(service.isEmpty()).toBe(true);
  });

  it('cantidad 0 o remove() elimina la línea', () => {
    service.add(AUDIFONOS);
    service.decrement(1);
    expect(service.isEmpty()).toBe(true);
    service.add(MOUSE);
    service.remove(2);
    expect(service.isEmpty()).toBe(true);
  });

  it('toRequest() arma el CartRequest con couponCode null por defecto', () => {
    service.add(AUDIFONOS);
    service.setQuantity(MOUSE, 3);
    expect(service.toRequest()).toEqual({
      items: [
        { productId: 1, quantity: 1 },
        { productId: 2, quantity: 3 },
      ],
      couponCode: null,
    });
  });

  it('applyCoupon() normaliza el código y cotiza con POST /cart/quote', () => {
    service.add(AUDIFONOS);
    service.applyCoupon('  WELCOME2026 ');
    expect(service.quoteLoading()).toBe(true);

    vi.advanceTimersByTime(REQUOTE_DEBOUNCE_MS);
    const req = http.expectOne(QUOTE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ items: [{ productId: 1, quantity: 1 }], couponCode: 'WELCOME2026' });

    req.flush(quoteResponse({ couponCode: 'WELCOME2026' }));
    expect(service.quote()?.total).toBe(96.9);
    expect(service.quoteLoading()).toBe(false);
  });

  it('un cambio en el carrito invalida el desglose y recotiza automáticamente', () => {
    service.add(AUDIFONOS);
    service.applyCoupon(null);
    vi.advanceTimersByTime(REQUOTE_DEBOUNCE_MS);
    http.expectOne(QUOTE_URL).flush(quoteResponse());
    expect(service.quote()).not.toBeNull();

    service.increment(1);
    expect(service.quote()).toBeNull();
    vi.advanceTimersByTime(REQUOTE_DEBOUNCE_MS);
    const req = http.expectOne(QUOTE_URL);
    expect(req.request.body.items).toEqual([{ productId: 1, quantity: 2 }]);
    req.flush(quoteResponse());
  });

  it('muestra el error del backend (cupón inválido) y detiene la recotización', () => {
    service.add(AUDIFONOS);
    service.applyCoupon('EXPIRED2025');
    vi.advanceTimersByTime(REQUOTE_DEBOUNCE_MS);
    http
      .expectOne(QUOTE_URL)
      .flush(
        { status: 422, title: 'Unprocessable Entity', detail: 'El cupón EXPIRED2025 está vencido' },
        { status: 422, statusText: 'Unprocessable Entity' },
      );

    expect(service.quote()).toBeNull();
    expect(service.quoteError()).toBe('El cupón EXPIRED2025 está vencido');

    service.increment(1);
    vi.advanceTimersByTime(REQUOTE_DEBOUNCE_MS);
    http.expectNone(QUOTE_URL);
  });

  it('syncProducts() ajusta cantidades al nuevo stock y quita productos agotados', () => {
    service.setQuantity(AUDIFONOS, 10);
    service.add(MOUSE);
    service.syncProducts([{ ...AUDIFONOS, stock: 4 }, { ...MOUSE, stock: 0 }]);
    expect(service.quantityOf(1)).toBe(4);
    expect(service.quantityOf(2)).toBe(0);
  });

  it('clear() vacía carrito, cupón y cotización', () => {
    service.add(AUDIFONOS);
    service.applyCoupon('WELCOME2026');
    service.clear();
    vi.advanceTimersByTime(REQUOTE_DEBOUNCE_MS);
    http.expectNone(QUOTE_URL);
    expect(service.isEmpty()).toBe(true);
    expect(service.couponCode()).toBeNull();
    expect(service.quote()).toBeNull();
  });
});
