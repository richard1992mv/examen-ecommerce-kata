import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { quoteResponse } from '../../testing/fixtures';
import { DiscountBreakdown, DiscountSummary } from './discount-summary';

describe('DiscountSummary (HU2/HU4)', () => {
  const render = (data: DiscountBreakdown): HTMLElement => {
    const fixture = TestBed.createComponent(DiscountSummary);
    fixture.componentRef.setInput('data', data);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  it('muestra el desglose y el detalle por línea sin alerta de tope', () => {
    const el = render(quoteResponse());

    expect(el.textContent).toContain('Descuento por Volumen');
    expect(el.textContent).toContain('$96.90');
    expect(el.textContent).toContain('Audífonos Bluetooth');
    expect(el.querySelector('app-cap-alert')).toBeNull();
    expect(el.querySelector('tr.table-warning')).toBeNull();
  });

  it('con tope aplicado muestra la alerta, el ajuste y el cupón', () => {
    const el = render(
      quoteResponse({
        capApplied: true,
        capAdjustment: 13.16,
        couponCode: 'TESTCAP30',
        message: '¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)',
      }),
    );

    expect(el.querySelector('app-cap-alert')?.textContent).toContain('¡Enhorabuena!');
    expect(el.querySelector('tr.table-warning')?.textContent).toContain('Ajuste por tope (35%)');
    expect(el.textContent).toContain('TESTCAP30');
  });
});
