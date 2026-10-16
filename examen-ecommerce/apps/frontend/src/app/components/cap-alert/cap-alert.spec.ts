import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { CapAlert } from './cap-alert';

describe('CapAlert (HU4)', () => {
  const render = (message: string | null): HTMLElement => {
    const fixture = TestBed.createComponent(CapAlert);
    fixture.componentRef.setInput('message', message);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  it('muestra el mensaje que envía el backend', () => {
    expect(render('¡Tope alcanzado!').textContent).toContain('¡Tope alcanzado!');
  });

  it('sin mensaje muestra el texto de respaldo del tope del 35 %', () => {
    expect(render(null).textContent).toContain('límite máximo de ahorro permitido (35%)');
  });
});
