import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API, orderResponse, product } from '../testing/fixtures';
import { CartService } from './cart.service';
import { OrderService } from './order.service';

describe('OrderService', () => {
  let service: OrderService;
  let cart: CartService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrderService);
    cart = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('checkout() con el carrito vacío no llama al backend', () => {
    service.checkout();
    http.expectNone(API.orders);
    expect(service.loading()).toBe(false);
  });

  it('checkout() envía el carrito, guarda la orden, vacía el carrito y recarga el stock (HU3)', () => {
    cart.setQuantity(product(1, 45, 25), 2);
    service.checkout();
    expect(service.loading()).toBe(true);

    const req = http.expectOne({ method: 'POST', url: API.orders });
    expect(req.request.body).toEqual({ items: [{ productId: 1, quantity: 2 }], couponCode: null });
    req.flush(orderResponse());

    expect(service.lastOrder()?.orderNumber).toBe('ORD-2026-000005');
    expect(service.loading()).toBe(false);
    expect(cart.isEmpty()).toBe(true);
    http.expectOne(API.products).flush([]);
  });

  it('checkout() no envía una segunda orden mientras la primera está en curso', () => {
    cart.setQuantity(product(1, 45, 25), 1);
    service.checkout();
    service.checkout();

    http.expectOne(API.orders).flush(orderResponse());
    http.expectOne(API.products).flush([]);
  });

  it('un 409 de stock muestra el error, conserva el carrito y recarga el catálogo', () => {
    cart.setQuantity(product(7, 55, 2, 'Hogar'), 2);
    service.checkout();
    http
      .expectOne(API.orders)
      .flush(
        { detail: 'Stock insuficiente', productId: 7, requested: 2, available: 1 },
        { status: 409, statusText: 'Conflict' },
      );

    expect(service.error()).toBe('Stock insuficiente (producto 7: solicitado 2, disponible 1)');
    expect(service.loading()).toBe(false);
    expect(cart.isEmpty()).toBe(false);
    http.expectOne(API.products).flush([]);
  });

  it('dismissConfirmation() oculta la confirmación', () => {
    service.lastOrder.set(orderResponse());
    service.dismissConfirmation();
    expect(service.lastOrder()).toBeNull();
  });
});
