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

## Autenticación (JWT)
El backend exige `Authorization: Bearer <token>` en todo `/api/v1` salvo el login. `src/app/services/auth.interceptor.ts` lo resuelve sin pantalla de login:
- `AuthTokenStore` llama a `POST /api/v1/auth/login` con un usuario de demostración del seed (`app_users`) y guarda el token **en memoria** (no en `localStorage`). Lo renueva 30 s antes de que expire.
- `authInterceptor` (registrado en `app.config.ts`) añade el encabezado a las llamadas a `environment.apiUrl`. Ante un 401 descarta el token, pide uno nuevo y reintenta una sola vez.
- El login usa `HttpBackend` para no pasar por el propio interceptor.

Pendiente: las credenciales de demostración están fijas en `auth.interceptor.ts`; no hay pantalla de login ni manejo de usuarios.

## Tipado estricto
- `tsconfig.json`: `strict`, `noImplicitReturns`, `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noFallthroughCasesInSwitch`; Angular con `strictTemplates`, `strictInjectionParameters` y `strictInputAccessModifiers`.
- **No hay `any`** en el código ni en las plantillas (tampoco `$any`). Los inputs se leen con referencias de plantilla tipadas (`#couponInput`, `#quantityInput`) en lugar de castear `event.target`.
- Los contratos de la API están en `src/app/models` y reflejan campo por campo los DTO del backend (`ProductResponse`, `CartRequest`, `CartQuoteResponse`, `OrderResponse`, `ApiProblem`, `LoginRequest`, `TokenResponse`). Los montos `BigDecimal` llegan como `number` en JSON.
- Uso justificado de `unknown`: los errores de RxJS y de `Promise.catch` no están tipados, así que se reciben como `unknown` y se acotan con `instanceof HttpErrorResponse`. El body del error (`HttpErrorResponse.error`, declarado `any` por Angular) se acota a `ApiProblem` con la guarda de tipo `isApiProblem` en `http-error.util.ts`.
- `CartService` modela el resultado de cada cotización como una unión discriminada (`QuoteOutcome`), sin `null` casteado.

## Pruebas unitarias (Vitest) y cobertura
```bash
npm test -- --watch=false     # ejecuta las pruebas una vez
npm run test:coverage         # pruebas + cobertura; falla si algún indicador baja del 80 %
```

- 12 archivos de prueba (55 pruebas): servicios (`CartService`, `ProductService`, `OrderService`), `authInterceptor`, `toErrorMessage` y todos los componentes, con `HttpTestingController` (sin backend real). Los datos de prueba compartidos están en `src/app/testing/fixtures.ts`.
- La cobertura usa `@vitest/coverage-v8` y se configura en `angular.json` → `test.options`: incluye **todo** `src/app/**/*.ts` (no solo los archivos que importan las pruebas) y excluye los specs, `testing/`, las interfaces de `models/`, el reexport de `icons.ts` y `app.config.ts`. `coverageThresholds` exige el 80 % en sentencias, ramas, funciones y líneas.
- Reporte en consola y HTML en `coverage/frontend/index.html` (además de `lcov.info` para CI).
- Cobertura actual: **sentencias 99.3 %, ramas 96.6 %, funciones 100 %, líneas 99.0 %**.

## Estilos e íconos
- Bootstrap: solo CSS, cargado en `angular.json` → `styles` (`node_modules/bootstrap/dist/css/bootstrap.min.css`). No se usa el JS de Bootstrap.
- Font Awesome: componente standalone `<fa-icon [icon]="...">`. Los íconos se importan de `src/app/icons.ts` (tree-shaking: solo entran al build los usados). Para agregar uno: exportarlo en `icons.ts` e importarlo en el componente.
- Ajustes propios mínimos en `src/styles.css`.

## Estructura
```
src/app
├─ models/        ProductResponse, CartRequest, CartQuoteResponse, OrderResponse, ApiProblem, LoginRequest, TokenResponse
├─ services/      ProductService, CartService (signals + recotización automática), OrderService, auth.interceptor (JWT), http-error.util
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
