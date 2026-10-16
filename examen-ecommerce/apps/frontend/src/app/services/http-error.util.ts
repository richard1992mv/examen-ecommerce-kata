import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem } from '../models';

/**
 * Convierte un error HTTP en un mensaje legible usando el body (Problem Details) del backend.
 *
 * Recibe `unknown` (no `any`): RxJS no tipa los errores, así que primero se comprueba que sea un
 * HttpErrorResponse antes de leer sus campos.
 */
export function toErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado.';
  }
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.';
  }

  const body: ApiProblem = isApiProblem(error.error) ? error.error : {};
  let message = body.detail || body.title || `Error ${error.status}`;

  if (body.errors?.length) {
    message += ': ' + body.errors.map((e) => `${e.field} ${e.message}`).join('; ');
  }
  if (error.status === 409 && body.productId != null) {
    message += ` (producto ${body.productId}: solicitado ${body.requested}, disponible ${body.available})`;
  }
  return message;
}

/**
 * Guarda de tipo para el body del error. Angular declara `HttpErrorResponse.error` como `any`, porque
 * el body depende del servidor; aquí se acota a objeto y, como todos los campos de ApiProblem son
 * opcionales, cualquier objeto JSON es un ApiProblem válido.
 */
function isApiProblem(body: unknown): body is ApiProblem {
  return typeof body === 'object' && body !== null;
}
