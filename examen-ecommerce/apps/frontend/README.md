# Frontend · Carrito de compras (Angular 22)

UI con **Bootstrap 5.3** y **Font Awesome 7** (`@fortawesome/angular-fontawesome`) que consume el backend Spring Boot en `http://localhost:8080/api/v1`.

## Requisitos
- Node.js **>= 22.22.3** o **>= 24.15** (exigido por Angular CLI 22)
- Backend corriendo en `localhost:8080` y MySQL con el seed cargado

## Cómo se creó
```bash
cd apps
npx @angular/cli@22 new frontend --style=css --skip-git --ssr=false --defaults
```

## Ejecutar
```bash
cd apps/frontend
npm install
npm start            # ng serve con proxy.conf.json → http://localhost:4200
```
`proxy.conf.json` redirige `/api` a `http://localhost:8080`, así no se necesita CORS en desarrollo.
La URL base se cambia en `src/environments/environment*.ts` (`apiUrl`).

## Pruebas unitarias (Vitest)
```bash
npm test -- --watch=false
```

## Estilos e íconos
- Bootstrap: solo CSS, cargado en `angular.json` → `styles` (`node_modules/bootstrap/dist/css/bootstrap.min.css`). No se usa el JS de Bootstrap.
- Font Awesome: componente standalone `<fa-icon [icon]="...">`. Los íconos se importan de `src/app/icons.ts` (tree-shaking: solo entran al build los usados). Para agregar uno: exportarlo en `icons.ts` e importarlo en el componente.
- Ajustes propios mínimos en `src/styles.css`.

## Estructura
```
src/app
├─ models/        ProductResponse, CartRequest, CartQuoteResponse, OrderResponse, ApiProblem
├─ services/      ProductService, CartService (signals + recotización automática), OrderService, http-error.util
└─ components/    product-list, cart, coupon-form, discount-summary, cap-alert, order-confirmation
```

## Flujo
1. HU1: "Agregar" suma 1 unidad (sin superar stock); el subtotal original se calcula en el cliente.
2. HU2: "Aplicar" (con o sin cupón) llama a `POST /cart/quote`. Desde ese momento cada cambio del carrito recotiza automáticamente (debounce 300 ms). Si el backend responde error (422 cupón, 409 stock…), se muestra y se detiene la recotización.
3. HU3: "Finalizar compra" llama a `POST /orders`, muestra la confirmación, vacía el carrito y recarga productos.
4. HU4: si `capApplied = true` se muestra la alerta con `message` (o el texto fijo).

## Casos para probar (seed)
| Caso | Cupón | Resultado esperado |
|---|---|---|
| HU2 | `WELCOME2026` | desglose con cupón |
| HU2 error | `EXPIRED2025` / `INACTIVE10` / `XYZ` | mensaje 422 del backend |
| HU4 | `TESTCAP30` con subtotal alto (p. ej. 320) | alerta de tope 35 % |
| HU3 error | producto con poco stock comprado en otra pestaña | 409 stock insuficiente |
