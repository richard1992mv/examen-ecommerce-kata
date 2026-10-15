import { Component, effect, inject, untracked } from '@angular/core';
import { Cart } from './components/cart/cart';
import { CouponForm } from './components/coupon-form/coupon-form';
import { DiscountSummary } from './components/discount-summary/discount-summary';
import { OrderConfirmation } from './components/order-confirmation/order-confirmation';
import { ProductList } from './components/product-list/product-list';
import { CartService } from './services/cart.service';
import { OrderService } from './services/order.service';
import { ProductService } from './services/product.service';

@Component({
  selector: 'app-root',
  imports: [ProductList, Cart, CouponForm, DiscountSummary, OrderConfirmation],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly products = inject(ProductService);
  protected readonly cart = inject(CartService);
  protected readonly orders = inject(OrderService);

  constructor() {
    this.products.load();
    // Cada recarga del catálogo actualiza precio/stock de lo que hay en el carrito
    effect(() => {
      const catalog = this.products.products();
      untracked(() => this.cart.syncProducts(catalog));
    });
  }
}
