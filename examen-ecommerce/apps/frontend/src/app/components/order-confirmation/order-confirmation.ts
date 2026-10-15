import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { OrderResponse } from '../../models';
import { DiscountSummary } from '../discount-summary/discount-summary';

@Component({
  selector: 'app-order-confirmation',
  imports: [DatePipe, DiscountSummary],
  templateUrl: './order-confirmation.html',
})
export class OrderConfirmation {
  readonly order = input.required<OrderResponse>();
  readonly closed = output<void>();
}
