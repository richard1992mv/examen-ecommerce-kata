import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleCheck, faCircleExclamation, faTicket, faXmark } from '../../icons';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-coupon-form',
  imports: [FaIconComponent],
  templateUrl: './coupon-form.html',
})
export class CouponForm {
  protected readonly cart = inject(CartService);
  protected readonly code = signal('');
  protected readonly icons = { faCircleCheck, faCircleExclamation, faTicket, faXmark };

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
