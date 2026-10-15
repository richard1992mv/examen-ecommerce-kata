import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { ProductResponse } from '../../models';
import { CartService } from '../../services/cart.service';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe],
  templateUrl: './product-list.html',
})
export class ProductList {
  protected readonly productService = inject(ProductService);
  protected readonly cart = inject(CartService);
  protected readonly currency = environment.defaultCurrency;

  protected canAdd(product: ProductResponse): boolean {
    return this.cart.quantityOf(product.id) < product.stock;
  }
}
