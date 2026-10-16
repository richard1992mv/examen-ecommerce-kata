import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API, product } from '../testing/fixtures';
import { ProductService } from './product.service';

describe('ProductService', () => {
  let service: ProductService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('load() obtiene el catálogo con GET /products (HU1)', () => {
    service.load();
    expect(service.loading()).toBe(true);

    http.expectOne({ method: 'GET', url: API.products }).flush([product(1, 45, 25)]);

    expect(service.products()).toEqual([product(1, 45, 25)]);
    expect(service.loading()).toBe(false);
    expect(service.error()).toBeNull();
  });

  it('load() muestra el error del backend y conserva el catálogo anterior', () => {
    service.load();
    http.expectOne(API.products).flush(null, { status: 0, statusText: 'Unknown Error' });

    expect(service.error()).toContain('No se pudo conectar con el servidor');
    expect(service.loading()).toBe(false);
    expect(service.products()).toEqual([]);
  });
});
