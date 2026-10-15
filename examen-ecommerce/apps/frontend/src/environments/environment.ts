export const environment = {
  production: true,
  /** URL base del backend. Ruta relativa: en producción se sirve detrás del mismo dominio. */
  apiUrl: '/api/v1',
  /** Moneda para precios de catálogo (GET /products no trae moneda). */
  defaultCurrency: 'USD',
};
