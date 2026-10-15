import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { CartQuoteResponse } from '../../models';
import { CapAlert } from '../cap-alert/cap-alert';

/** Campos comunes de CartQuoteResponse y OrderResponse. */
export type DiscountBreakdown = Pick<
  CartQuoteResponse,
  | 'subtotalOriginal'
  | 'categoryDiscount'
  | 'volumeDiscount'
  | 'couponDiscount'
  | 'capAdjustment'
  | 'discountTotal'
  | 'effectiveDiscountPct'
  | 'discountCapPct'
  | 'capApplied'
  | 'message'
  | 'total'
  | 'currency'
  | 'couponCode'
  | 'items'
>;

@Component({
  selector: 'app-discount-summary',
  imports: [CurrencyPipe, DecimalPipe, CapAlert],
  templateUrl: './discount-summary.html',
})
export class DiscountSummary {
  readonly data = input.required<DiscountBreakdown>();
}
