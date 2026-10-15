/** Problem Details (RFC 9457) que devuelve el GlobalExceptionHandler del backend. */
export interface ApiProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: { field: string; message: string }[];
  // 409 stock insuficiente
  productId?: number;
  requested?: number;
  available?: number;
  // 422 cupón inválido
  couponCode?: string;
}
