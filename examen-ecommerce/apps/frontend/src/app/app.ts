import { Component, effect, inject, untracked } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { Cart } from './components/cart/cart';
import { CouponForm } from './components/coupon-form/coupon-form';
import { DiscountSummary } from './components/discount-summary/discount-summary';
import { OrderConfirmation } from './components/order-confirmation/order-confirmation';
import { ProductList } from './components/product-list/product-list';
import { faCartShopping, faCircleInfo, faPercent, faStore } from './icons';
import { CartService } from './services/cart.service';
import { OrderService } from './services/order.service';
import { ProductService } from './services/product.service';

@Component({
  selector: 'app-root',
  imports: [FaIconComponent, ProductList, Cart, CouponForm, DiscountSummary, OrderConfirmation],
  templateUrl: './app.html',
})
export class App {
  protected readonly products = inject(ProductService);
  protected readonly cart = inject(CartService);
  protected readonly orders = inject(OrderService);
  protected readonly icons = { faCartShopping, faCircleInfo, faPercent, faStore };

  constructor() {
    this.products.load();
    // Cada recarga del catálogo actualiza precio/stock de lo que hay en el carrito
    effect(() => {
      const catalog = this.products.products();
      untracked(() => this.cart.syncProducts(catalog));
    });
  }
}
