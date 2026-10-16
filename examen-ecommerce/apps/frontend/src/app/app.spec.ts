import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { App } from './app';
import { CartService } from './services/cart.service';
import { OrderService } from './services/order.service';
import { API, orderResponse, product, quoteResponse } from './testing/fixtures';

describe('App', () => {
  let fixture: ComponentFixture<App>;
  let cart: CartService;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    cart = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(App);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('carga el catálogo al iniciar y sincroniza el carrito con el stock recibido', () => {
    cart.setQuantity(product(1, 45, 25), 10);
    http.expectOne(API.products).flush([product(1, 45, 4)]);
    fixture.detectChanges();

    expect(cart.quantityOf(1)).toBe(4);
    expect(el.querySelector('.navbar .badge')?.textContent).toContain('4');
  });

  it('con productos y sin cotizar invita a pulsar «Aplicar»', () => {
    const audifonos = product(1, 45, 25);
    http.expectOne(API.products).flush([audifonos]);
    cart.add(audifonos);
    fixture.detectChanges();
    expect(el.textContent).toContain('Pulse «Aplicar»');
  });

  it('muestra "Calculando…" y luego el desglose de la cotización', () => {
    http.expectOne(API.products).flush([]);
    cart.quoteLoading.set(true);
    fixture.detectChanges();
    expect(el.textContent).toContain('Calculando descuentos');

    cart.quoteLoading.set(false);
    cart.quote.set(quoteResponse());
    fixture.detectChanges();
    expect(el.querySelector('app-discount-summary')).not.toBeNull();
  });

  it('muestra la confirmación de la orden y la oculta al cerrarla', () => {
    http.expectOne(API.products).flush([]);
    const orders = TestBed.inject(OrderService);
    orders.lastOrder.set(orderResponse());
    fixture.detectChanges();
    expect(el.querySelector('app-order-confirmation')?.textContent).toContain('ORD-2026-000005');

    (el.querySelector('app-order-confirmation .btn-close') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('app-order-confirmation')).toBeNull();
  });
});
