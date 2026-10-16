import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { TokenResponse } from '../models';
import { API } from '../testing/fixtures';
import { authInterceptor } from './auth.interceptor';

const token = (accessToken: string, expiresIn = 3600): TokenResponse => ({
  accessToken,
  tokenType: 'Bearer',
  expiresIn,
});

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('inicia sesión con las credenciales de demo y agrega "Authorization: Bearer"', () => {
    client.get(API.products).subscribe();

    const login = http.expectOne({ method: 'POST', url: API.login });
    expect(login.request.body).toEqual({ username: 'admin', password: 'admin123' });
    login.flush(token('t1'));

    const req = http.expectOne(API.products);
    expect(req.request.headers.get('Authorization')).toBe('Bearer t1');
    req.flush([]);
  });

  it('reutiliza el token en memoria mientras no expire', () => {
    client.get(API.products).subscribe();
    http.expectOne(API.login).flush(token('t1'));
    http.expectOne(API.products).flush([]);

    client.get(API.products).subscribe();
    http.expectNone(API.login);
    expect(http.expectOne(API.products).request.headers.get('Authorization')).toBe('Bearer t1');
  });

  it('pide un token nuevo si el actual está dentro del margen de expiración', () => {
    client.get(API.products).subscribe();
    http.expectOne(API.login).flush(token('casi-vencido', 10));
    http.expectOne(API.products).flush([]);

    client.get(API.products).subscribe();
    http.expectOne(API.login).flush(token('t2'));
    expect(http.expectOne(API.products).request.headers.get('Authorization')).toBe('Bearer t2');
  });

  it('ante un 401 descarta el token, inicia sesión de nuevo y reintenta una vez', () => {
    client.get(API.products).subscribe();
    http.expectOne(API.login).flush(token('vencido'));
    http.expectOne(API.products).flush(null, { status: 401, statusText: 'Unauthorized' });

    http.expectOne(API.login).flush(token('nuevo'));
    const retry = http.expectOne(API.products);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer nuevo');
    retry.flush([]);
  });

  it('propaga los errores que no son 401 sin reintentar', () => {
    let status = 0;
    client.get(API.products).subscribe({
      error: (err: unknown) => (status = err instanceof HttpErrorResponse ? err.status : -1),
    });
    http.expectOne(API.login).flush(token('t1'));
    http.expectOne(API.products).flush(null, { status: 500, statusText: 'Server Error' });

    expect(status).toBe(500);
    http.expectNone(API.login);
  });

  it('no agrega token a URLs fuera de la API', () => {
    client.get('/assets/config.json').subscribe();
    http.expectNone(API.login);
    expect(http.expectOne('/assets/config.json').request.headers.has('Authorization')).toBe(false);
  });
});
