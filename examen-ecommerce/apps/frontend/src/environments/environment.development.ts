export const environment = {
  production: false,
  /**
   * '/api/v1' pasa por proxy.conf.json → http://localhost:8080 (evita CORS).
   * Para llamar directo al backend usar 'http://localhost:8080/api/v1' (requiere CORS habilitado).
   */
  apiUrl: '/api/v1',
  defaultCurrency: 'USD',
};
