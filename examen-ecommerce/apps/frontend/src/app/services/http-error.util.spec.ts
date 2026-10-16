import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { toErrorMessage } from './http-error.util';

const httpError = (status: number, error: unknown): HttpErrorResponse =>
  new HttpErrorResponse({ status, error });

describe('toErrorMessage', () => {
  it('un error que no es HTTP devuelve un mensaje genérico', () => {
    expect(toErrorMessage(new Error('boom'))).toBe('Ocurrió un error inesperado.');
  });

  it('status 0 indica que el backend no está disponible', () => {
    expect(toErrorMessage(httpError(0, null))).toContain('No se pudo conectar con el servidor');
  });

  it('usa el detail del Problem Details', () => {
    expect(toErrorMessage(httpError(422, { detail: "El cupón 'XYZ' no existe" }))).toBe(
      "El cupón 'XYZ' no existe",
    );
  });

  it('sin detail usa el title, y sin body usa el código HTTP', () => {
    expect(toErrorMessage(httpError(404, { title: 'Not Found' }))).toBe('Not Found');
    expect(toErrorMessage(httpError(500, 'texto plano'))).toBe('Error 500');
  });

  it('agrega los errores de validación por campo (400)', () => {
    const body = {
      detail: 'La solicitud contiene datos inválidos',
      errors: [{ field: 'items[0].quantity', message: 'debe ser mayor que 0' }],
    };
    expect(toErrorMessage(httpError(400, body))).toBe(
      'La solicitud contiene datos inválidos: items[0].quantity debe ser mayor que 0',
    );
  });

  it('agrega solicitado y disponible en el 409 de stock insuficiente', () => {
    const body = { detail: 'Stock insuficiente', productId: 7, requested: 5, available: 2 };
    expect(toErrorMessage(httpError(409, body))).toBe(
      'Stock insuficiente (producto 7: solicitado 5, disponible 2)',
    );
  });
});
