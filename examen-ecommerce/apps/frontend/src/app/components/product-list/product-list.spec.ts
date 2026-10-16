import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CartService } from '../../services/cart.service';
import { ProductService } from '../../services/product.service';
import { API, product } from '../../testing/fixtures';
import { ProductList } from './product-list';

describe('ProductList (HU1)', () => {
  let fixture: ComponentFixture<ProductList>;
  let products: ProductService;
  let cart: CartService;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    products = TestBed.inject(ProductService);
    cart = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProductList);
    el = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const rows = (): HTMLTableRowElement[] => Array.from(el.querySelectorAll('tbody tr'));
  const addButton = (row: HTMLTableRowElement): HTMLButtonElement =>
    row.querySelector('button') as HTMLButtonElement;

  it('muestra el spinner mientras carga el catálogo por primera vez', () => {
    products.loading.set(true);
    fixture.detectChanges();
    expect(el.textContent).toContain('Cargando productos');
  });

  it('lista los productos con badge de stock y "Agregar" suma al carrito', () => {
    products.products.set([product(1, 45, 25), product(4, 12, 3), product(9, 30, 0)]);
    fixture.detectChanges();

    const [normal, bajo, agotado] = rows();
    expect(normal.querySelector('.badge.text-bg-success')).not.toBeNull();
    expect(bajo.querySelector('.badge.text-bg-warning')).not.toBeNull();
    expect(agotado.textContent).toContain('Agotado');
    expect(addButton(agotado).disabled).toBe(true);

    addButton(normal).click();
    fixture.detectChanges();
    expect(cart.quantityOf(1)).toBe(1);
    expect(normal.textContent).toContain('1');
  });

  it('deshabilita "Agregar" cuando el carrito ya tiene todo el stock', () => {
    const ultimo = product(4, 12, 1);
    products.products.set([ultimo]);
    cart.add(ultimo);
    fixture.detectChanges();
    expect(addButton(rows()[0]).disabled).toBe(true);
  });

  it('sin productos muestra el mensaje vacío', () => {
    fixture.detectChanges();
    expect(el.textContent).toContain('No hay productos.');
  });

  it('ante un error muestra la alerta y "Reintentar" vuelve a cargar', () => {
    products.error.set('No se pudo conectar con el servidor.');
    fixture.detectChanges();
    expect(el.querySelector('.alert-danger')?.textContent).toContain('No se pudo conectar');

    (el.querySelector('.alert-danger button') as HTMLButtonElement).click();
    http.expectOne(API.products).flush([product(1, 45, 25)]);
    fixture.detectChanges();
    expect(el.querySelector('.alert-danger')).toBeNull();
  });
});
