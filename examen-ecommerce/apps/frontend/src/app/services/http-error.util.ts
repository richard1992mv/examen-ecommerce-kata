import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem } from '../models';

export const CAP_MESSAGE = '¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)';

/** Convierte un error HTTP en un mensaje legible usando el body (Problem Details) del backend. */
export function toErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado.';
  }
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.';
  }

  const body = (typeof error.error === 'object' && error.error !== null ? error.error : {}) as ApiProblem;
  let message = body.detail || body.title || `Error ${error.status}`;

  if (body.errors?.length) {
    message += ': ' + body.errors.map((e) => `${e.field} ${e.message}`).join('; ');
  }
  if (error.status === 409 && body.productId != null) {
    message += ` (producto ${body.productId}: solicitado ${body.requested}, disponible ${body.available})`;
  }
  return message;
}
