import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CartService } from '../../services/cart.service';
import { API, orderResponse, product } from '../../testing/fixtures';
import { Cart } from './cart';

describe('Cart (HU1/HU3)', () => {
  let fixture: ComponentFixture<Cart>;
  let cart: CartService;
  let http: HttpTestingController;
  let el: HTMLElement;

  const AUDIFONOS = product(1, 45, 25);
  const SARTENES = product(7, 55, 2, 'Hogar');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    cart = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Cart);
    el = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const button = (label: string): HTMLButtonElement =>
    el.querySelector(`button[aria-label="${label}"]`) as HTMLButtonElement;
  const quantityInput = (): HTMLInputElement =>
    el.querySelector('input[type="number"]') as HTMLInputElement;
  const checkoutButton = (): HTMLButtonElement =>
    el.querySelector('.btn-success') as HTMLButtonElement;

  it('vacío: muestra el mensaje y deshabilita "Finalizar compra"', () => {
    fixture.detectChanges();
    expect(el.textContent).toContain('El carrito está vacío.');
    expect(checkoutButton().disabled).toBe(true);
  });

  it('muestra las líneas y el subtotal original en tiempo real (HU1)', () => {
    cart.setQuantity(AUDIFONOS, 2);
    fixture.detectChanges();
    expect(el.querySelector('tfoot')?.textContent).toContain('$90.00');

    button('Aumentar').click();
    fixture.detectChanges();
    expect(cart.quantityOf(1)).toBe(3);
    expect(el.querySelector('tfoot')?.textContent).toContain('$135.00');

    button('Disminuir').click();
    button('Quitar').click();
    fixture.detectChanges();
    expect(cart.isEmpty()).toBe(true);
  });

  it('el input de cantidad se acota al stock disponible', () => {
    cart.add(SARTENES);
    fixture.detectChanges();

    const input = quantityInput();
    input.value = '9';
    input.dispatchEvent(new Event('change'));

    expect(cart.quantityOf(7)).toBe(2);
    expect(input.value).toBe('2');
  });

  it('"Vaciar" deja el carrito vacío', () => {
    cart.add(AUDIFONOS);
    fixture.detectChanges();
    (el.querySelector('.card-header button') as HTMLButtonElement).click();
    expect(cart.isEmpty()).toBe(true);
  });

  it('"Finalizar compra" crea la orden (HU3)', () => {
    cart.add(AUDIFONOS);
    fixture.detectChanges();
    checkoutButton().click();
    fixture.detectChanges();
    expect(el.textContent).toContain('Procesando');

    http.expectOne({ method: 'POST', url: API.orders }).flush(orderResponse());
    http.expectOne(API.products).flush([]);
    expect(cart.isEmpty()).toBe(true);
  });

  it('si la compra falla muestra el motivo', () => {
    cart.add(SARTENES);
    fixture.detectChanges();
    checkoutButton().click();
    http
      .expectOne(API.orders)
      .flush({ detail: 'Stock insuficiente' }, { status: 409, statusText: 'Conflict' });
    http.expectOne(API.products).flush([]);
    fixture.detectChanges();

    expect(el.querySelector('.alert-danger')?.textContent).toContain('Stock insuficiente');
  });
});
