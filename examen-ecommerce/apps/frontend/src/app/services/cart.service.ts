import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { EMPTY, Subject, catchError, debounceTime, map, of, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';
import { CartItem, CartQuoteResponse, CartRequest, ProductResponse } from '../models';
import { toErrorMessage } from './http-error.util';

/** Espera tras un cambio del carrito antes de recotizar. */
export const REQUOTE_DEBOUNCE_MS = 300;

/** Resultado de una cotización: unión discriminada por `ok` (sin castear null ni mezclar campos). */
type QuoteOutcome = { ok: true; quote: CartQuoteResponse } | { ok: false; error: unknown };

/**
 * Estado del carrito con signals.
 * El cliente SOLO calcula el subtotal original; los descuentos vienen de POST /cart/quote.
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cart/quote`;

  private readonly _items = signal<CartItem[]>([]);
  readonly items = this._items.asReadonly();

  /** Cupón enviado al backend (el último aplicado). */
  readonly couponCode = signal<string | null>(null);
  readonly quote = signal<CartQuoteResponse | null>(null);
  readonly quoteLoading = signal(false);
  readonly quoteError = signal<string | null>(null);

  /** true tras pulsar "Aplicar": cada cambio del carrito recotiza automáticamente. */
  private autoQuote = false;
  private readonly quoteTrigger$ = new Subject<void>();

  readonly isEmpty = computed(() => this._items().length === 0);
  readonly totalUnits = computed(() => this._items().reduce((acc, i) => acc + i.quantity, 0));
  /** Σ unitPrice × quantity, redondeado a 2 decimales. */
  readonly subtotalOriginal = computed(
    () =>
      Math.round(
        this._items().reduce((acc, i) => acc + i.product.unitPrice * i.quantity, 0) * 100,
      ) / 100,
  );

  constructor() {
    this.quoteTrigger$
      .pipe(
        debounceTime(REQUOTE_DEBOUNCE_MS),
        switchMap(() => {
          if (this.isEmpty()) return EMPTY;
          return this.http.post<CartQuoteResponse>(this.url, this.toRequest()).pipe(
            map((quote): QuoteOutcome => ({ ok: true, quote })),
            catchError((error: unknown) => of<QuoteOutcome>({ ok: false, error })),
          );
        }),
      )
      .subscribe((outcome) =>
        outcome.ok ? this.onQuote(outcome.quote) : this.onQuoteError(outcome.error),
      );
  }

  quantityOf(productId: number): number {
    return this._items().find((i) => i.product.id === productId)?.quantity ?? 0;
  }

  /** Agrega 1 unidad; si ya existe incrementa (el backend rechaza productId repetidos). */
  add(product: ProductResponse): void {
    this.setQuantity(product, this.quantityOf(product.id) + 1);
  }

  increment(productId: number): void {
    const item = this.find(productId);
    if (item) this.setQuantity(item.product, item.quantity + 1);
  }

  decrement(productId: number): void {
    const item = this.find(productId);
    if (item) this.setQuantity(item.product, item.quantity - 1);
  }

  /** Fija la cantidad acotada a [0, stock]. Cantidad 0 elimina la línea. */
  setQuantity(product: ProductResponse, quantity: number): void {
    const qty = Math.min(Math.max(Math.trunc(quantity) || 0, 0), product.stock);
    const exists = this.find(product.id);
    if ((exists?.quantity ?? 0) === qty) return;

    if (qty === 0) {
      this._items.update((items) => items.filter((i) => i.product.id !== product.id));
    } else if (exists) {
      this._items.update((items) =>
        items.map((i) => (i.product.id === product.id ? { ...i, quantity: qty } : i)),
      );
    } else {
      this._items.update((items) => [...items, { product, quantity: qty }]);
    }
    this.onCartChanged();
  }

  remove(productId: number): void {
    const item = this.find(productId);
    if (item) this.setQuantity(item.product, 0);
  }

  /**
   * Sincroniza el carrito con el catálogo recién cargado: actualiza precio/stock,
   * recorta cantidades que superen el stock y elimina productos inexistentes o agotados.
   */
  syncProducts(products: ProductResponse[]): void {
    const current = this._items();
    if (current.length === 0) return;
    const byId = new Map(products.map((p) => [p.id, p]));
    const next: CartItem[] = [];
    let quantitiesChanged = false;
    for (const item of current) {
      const fresh = byId.get(item.product.id);
      const qty = fresh ? Math.min(item.quantity, fresh.stock) : 0;
      if (qty !== item.quantity || fresh?.unitPrice !== item.product.unitPrice)
        quantitiesChanged = true;
      if (fresh && qty > 0) next.push({ product: fresh, quantity: qty });
    }
    this._items.set(next);
    if (quantitiesChanged) this.onCartChanged();
  }

  /** Aplica (o quita, con null/vacío) el cupón y solicita la cotización. */
  applyCoupon(code: string | null): void {
    const normalized = code?.trim() || null;
    this.couponCode.set(normalized);
    this.autoQuote = true;
    this.requestQuote();
  }

  /** Vacía el carrito y reinicia la cotización. */
  clear(): void {
    this._items.set([]);
    this.couponCode.set(null);
    this.autoQuote = false;
    this.resetQuote();
  }

  toRequest(): CartRequest {
    return {
      items: this._items().map((i) => ({ productId: i.product.id, quantity: i.quantity })),
      couponCode: this.couponCode(),
    };
  }

  private requestQuote(): void {
    this.quote.set(null);
    this.quoteError.set(null);
    if (this.isEmpty()) {
      this.quoteLoading.set(false);
      return;
    }
    this.quoteLoading.set(true);
    this.quoteTrigger$.next();
  }

  /** Cualquier cambio invalida el desglose; si ya se había cotizado, recotiza. */
  private onCartChanged(): void {
    if (this.autoQuote) {
      this.requestQuote();
    } else {
      this.resetQuote();
    }
  }

  private onQuote(quote: CartQuoteResponse): void {
    this.quote.set(quote);
    this.quoteLoading.set(false);
  }

  /** Error del backend (cupón inválido/vencido, stock, etc.): se muestra y se detiene la recotización. */
  private onQuoteError(error: unknown): void {
    this.quote.set(null);
    this.quoteError.set(toErrorMessage(error));
    this.quoteLoading.set(false);
    this.autoQuote = false;
  }

  private resetQuote(): void {
    this.quote.set(null);
    this.quoteError.set(null);
    this.quoteLoading.set(false);
  }

  private find(productId: number): CartItem | undefined {
    return this._items().find((i) => i.product.id === productId);
  }
}
