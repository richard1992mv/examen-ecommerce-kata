import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { OrderResponse } from '../models';
import { CartService } from './cart.service';
import { toErrorMessage } from './http-error.util';
import { ProductService } from './product.service';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly cart = inject(CartService);
  private readonly products = inject(ProductService);
  private readonly url = `${environment.apiUrl}/orders`;

  readonly lastOrder = signal<OrderResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /** POST /orders → muestra confirmación, vacía el carrito y recarga productos (nuevo stock). */
  checkout(): void {
    if (this.cart.isEmpty() || this.loading()) return;

    this.loading.set(true);
    this.error.set(null);
    this.http.post<OrderResponse>(this.url, this.cart.toRequest()).subscribe({
      next: (order) => {
        this.lastOrder.set(order);
        this.loading.set(false);
        this.cart.clear();
        this.products.load();
      },
      error: (err: unknown) => {
        this.error.set(toErrorMessage(err));
        this.loading.set(false);
        // p. ej. 409 stock insuficiente: refrescar catálogo para ver el stock real
        this.products.load();
      },
    });
  }

  dismissConfirmation(): void {
    this.lastOrder.set(null);
  }
}
