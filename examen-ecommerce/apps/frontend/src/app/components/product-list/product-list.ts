import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { environment } from '../../../environments/environment';
import { faArrowsRotate, faBoxOpen, faCartPlus, faCircleExclamation, faStore } from '../../icons';
import { ProductResponse } from '../../models';
import { CartService } from '../../services/cart.service';
import { ProductService } from '../../services/product.service';

/** Stock igual o inferior a este valor se muestra como "bajo". */
const LOW_STOCK = 5;

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, FaIconComponent],
  templateUrl: './product-list.html',
})
export class ProductList {
  protected readonly productService = inject(ProductService);
  protected readonly cart = inject(CartService);
  protected readonly currency = environment.defaultCurrency;
  protected readonly icons = { faArrowsRotate, faBoxOpen, faCartPlus, faCircleExclamation, faStore };

  protected canAdd(product: ProductResponse): boolean {
    return this.cart.quantityOf(product.id) < product.stock;
  }

  protected stockBadge(stock: number): string {
    if (stock === 0) return 'text-bg-danger';
    return stock <= LOW_STOCK ? 'text-bg-warning' : 'text-bg-success';
  }
}
