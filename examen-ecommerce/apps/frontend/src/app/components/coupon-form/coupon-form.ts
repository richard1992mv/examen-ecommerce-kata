import { Component, effect, inject, signal, untracked } from '@angular/core';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-coupon-form',
  templateUrl: './coupon-form.html',
})
export class CouponForm {
  protected readonly cart = inject(CartService);
  protected readonly code = signal('');

  constructor() {
    // Al vaciar el carrito (p. ej. tras finalizar la compra) se limpia el input
    effect(() => {
      if (this.cart.isEmpty()) untracked(() => this.code.set(''));
    });
  }

  protected apply(): void {
    this.cart.applyCoupon(this.code());
  }

  protected removeCoupon(): void {
    this.code.set('');
    this.cart.applyCoupon(null);
  }
}
