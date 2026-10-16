import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { orderResponse } from '../../testing/fixtures';
import { OrderConfirmation } from './order-confirmation';

describe('OrderConfirmation (HU3)', () => {
  it('muestra el número de orden y emite "closed" al cerrar', () => {
    const fixture = TestBed.createComponent(OrderConfirmation);
    fixture.componentRef.setInput('order', orderResponse());
    let closed = 0;
    fixture.componentInstance.closed.subscribe(() => closed++);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('ORD-2026-000005');
    expect(el.textContent).toContain('16/10/2026 10:30:00');

    el.querySelectorAll<HTMLButtonElement>('button').forEach((button) => button.click());
    expect(closed).toBe(2);
  });
});
