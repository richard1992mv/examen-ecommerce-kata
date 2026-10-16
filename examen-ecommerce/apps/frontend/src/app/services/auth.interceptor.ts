import {
  HttpBackend,
  HttpClient,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

const LOGIN_URL = `${environment.apiUrl}/auth/login`;
/** Credenciales del usuario en memoria del backend (app.security.user / app.security.password). */
const API_CREDENTIALS = { username: 'admin', password: 'admin123' } as const;
/** Margen para renovar el token antes de que expire (segundos). */
const EXPIRY_MARGIN_S = 30;

interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

/** Obtiene y guarda en memoria el JWT. Usa HttpBackend para que el login no pase por el interceptor. */
@Injectable({ providedIn: 'root' })
export class AuthTokenStore {
  private readonly http = new HttpClient(inject(HttpBackend));
  private token: string | null = null;
  private expiresAt = 0;
  private pending$: Observable<string> | null = null;

  getToken(): Observable<string> {
    if (this.token && Date.now() < this.expiresAt) {
      return of(this.token);
    }
    this.pending$ ??= this.http.post<TokenResponse>(LOGIN_URL, API_CREDENTIALS).pipe(
      map((res) => {
        this.token = res.accessToken;
        this.expiresAt = Date.now() + Math.max(res.expiresIn - EXPIRY_MARGIN_S, 0) * 1000;
        return res.accessToken;
      }),
      finalize(() => (this.pending$ = null)),
      shareReplay(1),
    );
    return this.pending$;
  }

  clear(): void {
    this.token = null;
    this.expiresAt = 0;
  }
}

/** Agrega "Authorization: Bearer <token>" a las llamadas a la API; ante un 401 renueva el token y reintenta una vez. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl) || req.url === LOGIN_URL) {
    return next(req);
  }
  const store = inject(AuthTokenStore);
  const withToken = (token: string): HttpRequest<unknown> =>
    req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

  return store.getToken().pipe(
    switchMap((token) => next(withToken(token))),
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401) {
        store.clear();
        return store.getToken().pipe(switchMap((token) => next(withToken(token))));
      }
      return throwError(() => err);
    }),
  );
};
