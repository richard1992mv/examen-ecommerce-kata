import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CartService, REQUOTE_DEBOUNCE_MS } from '../../services/cart.service';
import { API, product, quoteResponse } from '../../testing/fixtures';
import { CouponForm } from './coupon-form';

describe('CouponForm (HU2)', () => {
  let fixture: ComponentFixture<CouponForm>;
  let cart: CartService;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    cart = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CouponForm);
    el = fixture.nativeElement as HTMLElement;
    cart.add(product(1, 45, 25));
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  const input = (): HTMLInputElement => el.querySelector('#coupon') as HTMLInputElement;
  /** Simula escribir: el evento input actualiza el signal y la vista se refresca como en el navegador. */
  const type = (code: string): void => {
    input().value = code;
    input().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const submit = (): void => {
    el.querySelector('form')?.dispatchEvent(new Event('submit'));
    vi.advanceTimersByTime(REQUOTE_DEBOUNCE_MS);
  };

  it('aplica el cupón escrito y muestra la confirmación', () => {
    type('WELCOME2026');
    submit();

    const req = http.expectOne(API.quote);
    expect(req.request.body.couponCode).toBe('WELCOME2026');
    req.flush(quoteResponse({ couponCode: 'WELCOME2026' }));
    fixture.detectChanges();

    expect(el.querySelector('.alert-success')?.textContent).toContain('WELCOME2026');
  });

  it('muestra el error del cupón y marca el input como inválido', () => {
    type('XYZ');
    submit();
    http
      .expectOne(API.quote)
      .flush({ detail: "El cupón 'XYZ' no existe" }, { status: 422, statusText: 'Unprocessable' });
    fixture.detectChanges();

    expect(el.querySelector('.alert-danger')?.textContent).toContain("El cupón 'XYZ' no existe");
    expect(input().classList).toContain('is-invalid');
  });

  it('"Quitar cupón" limpia el input y recotiza sin cupón', () => {
    type('WELCOME2026');
    submit();
    http.expectOne(API.quote).flush(quoteResponse({ couponCode: 'WELCOME2026' }));
    fixture.detectChanges();

    (el.querySelector('button[aria-label="Quitar cupón"]') as HTMLButtonElement).click();
    vi.advanceTimersByTime(REQUOTE_DEBOUNCE_MS);
    expect(http.expectOne(API.quote).request.body.couponCode).toBeNull();
    fixture.detectChanges();
    expect(input().value).toBe('');
  });

  it('al vaciar el carrito se limpia el código escrito', () => {
    type('WELCOME2026');
    cart.clear();
    fixture.detectChanges();
    expect(input().value).toBe('');
  });
});
