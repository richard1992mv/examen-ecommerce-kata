import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleCheck, faReceipt, faXmark } from '../../icons';
import { OrderResponse } from '../../models';
import { DiscountSummary } from '../discount-summary/discount-summary';

@Component({
  selector: 'app-order-confirmation',
  imports: [DatePipe, FaIconComponent, DiscountSummary],
  templateUrl: './order-confirmation.html',
})
export class OrderConfirmation {
  readonly order = input.required<OrderResponse>();
  readonly closed = output<void>();
  protected readonly icons = { faCircleCheck, faReceipt, faXmark };
}
