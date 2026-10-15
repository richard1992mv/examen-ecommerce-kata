import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { environment } from '../../../environments/environment';
import {
  faBagShopping,
  faCartShopping,
  faCircleExclamation,
  faCreditCard,
  faMinus,
  faPlus,
  faTrashCan,
} from '../../icons';
import { CartItem } from '../../models';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe, FaIconComponent],
  templateUrl: './cart.html',
})
export class Cart {
  protected readonly cart = inject(CartService);
  protected readonly orders = inject(OrderService);
  protected readonly currency = environment.defaultCurrency;
  protected readonly icons = {
    faBagShopping,
    faCartShopping,
    faCircleExclamation,
    faCreditCard,
    faMinus,
    faPlus,
    faTrashCan,
  };

  protected onQuantityInput(item: CartItem, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cart.setQuantity(item.product, input.valueAsNumber);
    // Refleja el valor acotado (0..stock) si el usuario escribió algo fuera de rango
    input.value = String(this.cart.quantityOf(item.product.id) || '');
  }
}
