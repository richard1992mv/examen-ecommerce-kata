# Arquitectura · Backend examen-ecommerce

## 1. Por qué MVC por capas

El dominio es pequeño (5 tablas de negocio + `app_users` para autenticación) y su complejidad está en **una** zona: el cálculo de descuentos. Una arquitectura MVC por capas (Controller → Service → Repository) es suficiente, conocida por cualquier equipo Spring y fácil de evaluar. La parte compleja se aísla en `service/discount/` (`DiscountCalculator`), que opera sobre records propios (`DiscountLine` → `DiscountResult`) sin entidades JPA ni repositorios, de modo que el cálculo se prueba sin base de datos. No es Java puro: `DiscountCalculator` es un `@Component` que recibe `DiscountProperties` por constructor.

> **Estado actual.** Una primera versión generada incluía más paquetes (`adapter/`, `event/`, `service/impl/`, CRUD de catálogo y cupones). El commit `9a8c8a3` redujo el código al alcance de las HU1–HU4. Este documento describe el código vigente; lo que se eliminó o sigue pendiente se indica explícitamente.

## 2. Responsabilidad de cada capa

| Paquete | Responsabilidad | No hace |
|---|---|---|
| `controller/` | HTTP: valida la entrada (`@Valid`) y delega. `AuthController` (login), `ProductController` (`GET /products`), `CartController` (`POST /cart/quote`), `OrderController` (`POST /orders`, 201) | Lógica de negocio, acceso a repositorios |
| `dto/request`, `dto/response` | Records de entrada/salida. La entrada (`CartRequest`, `CartItemRequest`) no tiene `id`, timestamps ni montos (anti asignación masiva) | Exponer entidades |
| `dto/validation/` | `@UniqueProductIds` + `UniqueProductIdsValidator` (sin `productId` repetidos en el carrito) | — |
| `service/` | Casos de uso como clases concretas (sin interfaces): `PricingService`, `OrderService`, `StockService`, `ProductService`, `CartValidator`, `CouponValidator` | Conversión DTO/entidad (mapper), cálculo (motor) |
| `service/discount/` | Motor de descuentos: `DiscountCalculator`, `DiscountLine`, `DiscountResult` | Importar `entity` o `jakarta.persistence` |
| `mapper/` | `CheckoutMapper`: `DiscountResult` → respuesta de quote, entidad `Order` con snapshots y respuesta de orden; mensaje HU4 | Reglas de negocio |
| `entity/` | Mapeo JPA fiel a `01_schema.sql` (bases `IdentifiedEntity` y `AuditableEntity`) | Lógica de aplicación |
| `repository/` | Spring Data JPA, `JOIN FETCH`/`@EntityGraph`, UPDATE atómico de stock | — |
| `exception/` | Excepciones de negocio + único `GlobalExceptionHandler` (RFC 9457); mensajes en `ErrorMessages` | — |
| `security/` | JWT: `SecurityConfig` (cadena de filtros y CORS), `JwtService`, `JwtAuthenticationFilter`, `DatabaseUserDetailsService`, manejadores 401/403 | — |
| `config/` | `OpenApiConfig` (esquema `bearerAuth`), `DiscountProperties`, `OrderNumberProperties` | — |
| `util/` | `MoneyUtils` (todo el redondeo), `OrderNumberGenerator` | — |

Pendiente: no existe una prueba automática de arquitectura (pureza de paquetes, controllers sin repositorios). Hoy la inyección es por constructor en todas las clases, pero no hay nada que lo verifique.

## 3. Modelo de datos (tabla → entidad)

| Tabla | Entidad | Notas de mapeo |
|---|---|---|
| `categories` | `Category` | `discount_pct DECIMAL(5,2) UNSIGNED` → `BigDecimal` |
| `products` | `Product` | `@ManyToOne(LAZY) Category`; `stock INT UNSIGNED` → `Integer` |
| `coupons` | `Coupon` | `is_active TINYINT(1) UNSIGNED` → `boolean` con `@JdbcTypeCode(TINYINT)`; `isValidAt()` inclusivo |
| `orders` | `Order` (entidad JPA `PurchaseOrder`) | `ORDER` es palabra reservada en JPQL; `@ManyToOne(LAZY, optional) Coupon`; `@OneToMany(mappedBy, PERSIST)` + `addItem()`; `currency CHAR(3)` con `@JdbcTypeCode(CHAR)`; sin setters (inmutable) |
| `order_items` | `OrderItem` | `@ManyToOne(LAZY)` a `Order` y `Product`; snapshots `product_name`, `category_name`, `unit_price` |
| `app_users` | `AppUser` | Solo lectura (login JWT). `password_hash` BCrypt; `role` sin prefijo `ROLE_`; `is_active` con `@JdbcTypeCode(TINYINT)`; `toString` sin el hash |

- IDs `INT UNSIGNED` → `Integer` (`columnDefinition = "INT UNSIGNED"`), `DATETIME` → `LocalDateTime`, `DECIMAL` → `BigDecimal`.
- `created_at/updated_at` de catálogo y `app_users` los gestiona MySQL (`AuditableEntity`: `insertable=false, updatable=false`). En `orders.created_at` la aplicación escribe `LocalDateTime.now()` (coherente con el año del `order_number`). Pendiente: no hay `Clock` inyectado, así que la hora no es determinista en pruebas.
- `equals/hashCode` por id (compatibles con proxies) y `toString` sin relaciones.
- Anti N+1: `JOIN FETCH` (`findAllWithCategoryByIdIn`, usado en quote y checkout) y `@EntityGraph` (`findAllByOrderByIdAsc`, catálogo de HU1).

La tabla `app_users` se añadió para la autenticación JWT (`01_schema.sql` en instalaciones nuevas, `04_auth_users.sql` en bases ya existentes). Las tablas de negocio no cambiaron. Lo que requeriría esquema nuevo está en `docs/migraciones-propuestas.md` (sin aplicar).

## 4. Reglas de negocio (cascada multiplicativa)

Redondeo **HALF_UP a 2 decimales al final de cada paso**, igual que `02_seed_data.sql`. Todo pasa por `MoneyUtils`.

Las cuatro reglas se aplican en secuencia dentro de `DiscountCalculator.calculate()`; la validez del cupón la decide antes `CouponValidator`.

| Paso | Regla | Dónde |
|---|---|---|
| R1 | Por línea: `line_subtotal × categories.discount_pct`, redondeado por línea. `subtotal_1 = original − categoría` | `DiscountCalculator.applyCategory` |
| R2 | Si `subtotal_1 > 100.00` (estricto): `subtotal_1 × 5 %` | `DiscountCalculator.calculate` |
| R3 | Cupón válido (existe, activo, `valid_from ≤ ahora ≤ valid_until`): `subtotal_2 × coupons.discount_pct` | `CouponValidator` + `DiscountCalculator.calculate` |
| R4 | `bruto = R1+R2+R3`; `tope = original × 35 %`; si `bruto > tope` → ajuste `bruto − tope`, `cap_applied = 1` | `DiscountCalculator.calculate` |

`total = original − discount_total`; `effective_discount_pct = discount_total / original × 100`; `discount_cap_pct` = snapshot del tope. Porcentajes y umbral en `discount.volume.pct`, `discount.volume.threshold`, `discount.cap-pct`.

Ejemplos del seed (verificados al centavo por las pruebas unitarias `CategoryDiscountTest`, `VolumeDiscountTest`, `CouponDiscountTest` y `DiscountCapTest`, y por las pruebas con BD `CouponBreakdownTest`, `CheckoutOrderTest` y `DiscountCapAlertTest`):

| Caso | Carrito | Subtotal | Cat. | Vol. | Cupón | Ajuste | Dto. | Total | % ef. |
|---|---|---|---|---|---|---|---|---|---|
| (a) | Novela×2 + Mouse×1 | 50.00 | 2.00 | 0.00 | 0.00 | 0.00 | 2.00 | 48.00 | 4.00 |
| (b) | Teclado×1 + Lámpara×1 | 110.00 | 8.00 | 5.10 | 0.00 | 0.00 | 13.10 | 96.90 | 11.91 |
| (c) | Audífonos×2 + Libro prog.×1 + WELCOME2026 | 130.00 | 9.00 | 6.05 | 17.24 | 0.00 | 32.29 | 97.71 | 24.84 |
| (d) | Monitor + Smartwatch + Balón×2 + TESTCAP30 | 320.00 | 27.00 | 14.65 | 83.51 | 13.16 | 112.00 | 208.00 | 35.00 (HU4) |

Caso (d) paso a paso: 293.00 × 5 % = 14.65 → 278.35 × 30 % = 83.505 → **83.51** (HALF_UP) → bruto 125.16 > 112.00 → ajuste 13.16.

HU4: con `cap_applied = 1` la respuesta incluye `"¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)"`, armado por `CheckoutMapper.capMessage` a partir de `capApplied` y `discountCapPct` (determinista). Sin tope, `message` es `null`.

## 5. Flujos

```
POST /cart/quote                         POST /orders  (TransactionTemplate: una transacción por intento)
 CartController (@Valid)                  OrderController (@Valid) → 201
  └─ PricingService.quote                  └─ OrderService.checkout ── reintento acotado (UNIQUE order_number)
      └─ PricingService.price ◄── mismo ───────── placeOrder
          1 CartValidator                        1-5 PricingService.price (idéntico a quote)
          2 ProductRepository (1 consulta)       6   StockService.decrement → UPDATE ... WHERE stock >= :qty
          3 CouponValidator (LocalDateTime.now)  7   OrderNumberGenerator (ORD-2026-000005)
          4 StockService.assertAvailable         8   CheckoutMapper.toOrder → saveAndFlush
          5 DiscountCalculator                   9   CheckoutMapper.toOrderResponse
      └─ CheckoutMapper.toQuoteResponse
```

Todas las rutas bajo `/api/v1` (salvo `POST /auth/login`) pasan antes por `JwtAuthenticationFilter` (§11).

## 6. SOLID y DRY

| Principio | Clase(s) | Cómo se cumple |
|---|---|---|
| **S** Responsabilidad única | `CartValidator`, `CouponValidator`, `StockService`, `DiscountCalculator`, `CheckoutMapper`, `OrderNumberGenerator` | Validación del carrito, del cupón, stock, cálculo, conversión y numeración en clases separadas; controllers sin lógica |
| **O** Abierto/cerrado | `DiscountCalculator`, `DiscountProperties` | Parcial: los porcentajes y el umbral se cambian por configuración o en la BD sin tocar código. Añadir una regla nueva **sí** exige modificar `DiscountCalculator` (no hay abstracción por regla) |
| **L** Liskov | — | No aplica: no hay jerarquías de reglas intercambiables. Las únicas herencias son las bases de entidades (`IdentifiedEntity`, `AuditableEntity`) |
| **I** Segregación | `PricingService`, `StockService`, `CartValidator`, `CouponValidator` | Servicios pequeños por responsabilidad; no existe un `EcommerceService`. No se definen interfaces |
| **D** Inversión | Todas las clases de `service/`, `security/`, `mapper/` | Inyección por constructor y `DiscountProperties` inyectado. Las dependencias son clases concretas, no interfaces |
| **DRY** dinero | `MoneyUtils` | Único lugar con `setScale/RoundingMode` (`percentOf`, `ratioAsPercent`, …) |
| **DRY** cálculo | `PricingService` | `/cart/quote` y `/orders` ejecutan exactamente el mismo flujo |
| **DRY** errores | `GlobalExceptionHandler`, `ErrorMessages`, `SecurityProblems` | Problem Details se construye en un solo formato (también para 401/403); mensajes como constantes |
| **DRY** validación | `@UniqueProductIds` | `CartValidator` reutiliza `UniqueProductIdsValidator.firstDuplicate` |

## 7. Patrones de diseño

> ⚠️ **Pendiente frente al requisito de la kata (al menos dos patrones).** Los cuatro patrones de la primera versión (tabla siguiente) **no están implementados en el código actual**: se eliminaron en el commit `9a8c8a3`. Se conserva la tabla como diseño de referencia.

| Patrón | Problema que resuelve | Clases (eliminadas) | Alternativa descartada y por qué |
|---|---|---|---|
| **Strategy** | Cada descuento tiene su fórmula y condiciones; deben combinarse en cascada | `DiscountRule` + 3 implementaciones; `DiscountCapPolicy` + `PercentageCapPolicy` | `if/switch` por tipo en un servicio: viola OCP y mezcla responsabilidades |
| **Factory** | Crear las reglas correctas (con/sin cupón) con parámetros de configuración | `DiscountRuleFactory` (único `new` de reglas) | Beans `@Component` por regla + `List<DiscountRule>` inyectada: la regla de cupón depende del cupón de cada petición, no es un singleton |
| **Adapter** | Aislar el motor del modelo JPA | `DiscountContextAdapter`, `DiscountResultAdapter` | Pasar entidades al motor: acopla el cálculo a Hibernate (proxies LAZY, cambios de esquema) y exige BD para probar |
| **Observer** | Auditoría y métricas sin acoplar los servicios | `CartEventPublisher`, `CartEvent` (sealed) + 3 records, `CartAuditListener`, `DiscountMetricsListener` | Llamar logger/métricas desde los servicios: viola SRP; usar listeners para descontar stock: rompe la atomicidad del checkout |

Lo que sí existe hoy en el código:

| Elemento | Clases | Observación |
|---|---|---|
| **Builder** | `Order.builder()` (`Order.Builder`) | Construye la orden inmutable (sin setters) con todos los montos y snapshots |
| **Mapper / DTO** | `CheckoutMapper`, `ProductResponse.from` | Separa entidades y contrato HTTP |
| **Template Method de Spring** | `TransactionTemplate` en `OrderService` | Una transacción nueva por intento de checkout, para poder reintentar fuera de ella |
| **Filtro de cadena (Spring Security)** | `JwtAuthenticationFilter` en `SecurityConfig` | Autenticación por token antes de los controllers |

Si se decide recuperar Strategy + Factory, el punto de extensión natural es `DiscountCalculator.calculate()`, que hoy aplica las cuatro reglas en secuencia.

## 8. `ddl-auto=validate` con columnas UNSIGNED

**Causa.** `SchemaValidator` compara el tipo JDBC/nombre que reporta Connector/J con el que Hibernate espera para el atributo Java. MySQL reporta `INT UNSIGNED`, `DECIMAL UNSIGNED`, `TINYINT UNSIGNED` (o `BIT` si `tinyInt1isBit=true`) y `CHAR`, mientras Hibernate espera `integer`, `decimal`, `bit` (boolean) y `varchar` (String). Los decimales son equivalentes (`NUMERIC/DECIMAL`), pero boolean, `CHAR(3)` y potencialmente los enteros sin signo pueden fallar.

**Solución aplicada (sin cambiar a `update`):**

1. `columnDefinition = "INT UNSIGNED"` en ids, FKs, `stock` y `quantity`: el validador acepta la columna si su nombre de tipo empieza igual.
2. `boolean` con `@JdbcTypeCode(SqlTypes.TINYINT)` + `tinyInt1isBit=false` en la URL JDBC: ambos lados son `TINYINT`. Pendiente de verificar: la URL actual de `application.properties` no incluye `tinyInt1isBit=false` (solo `zeroDateTimeBehavior=convertToNull`). Si el contexto arranca con `validate`, el mapeo se acepta igualmente; conviene confirmarlo ejecutando las pruebas con BD.
3. `currency` con `@JdbcTypeCode(SqlTypes.CHAR)`.

Las pruebas `@SpringBootTest` (por ejemplo, `CheckoutOrderTest`) arrancan con `validate` contra la BD MySQL local del perfil `dev`; si el contexto arranca, la validación pasó.

**Dialecto.** Spring Boot 3.2.12 trae Hibernate ORM 6.4.x, cuyo `MySQLDialect` todavía soporta MySQL 5.7. Hibernate 6.6+ (Boot 3.4+) sube el mínimo a MySQL 8.0, por eso no se sube de versión.

## 9. Decisiones

| Decisión | Motivo |
|---|---|
| **UPDATE atómico de stock** (`... SET stock = stock - :qty WHERE id = :id AND stock >= :qty`) | Evita sobreventa sin bloqueos optimistas ni lecturas previas; 0 filas → `InsufficientStockException` → rollback total. Se ejecuta en orden de `productId` para minimizar deadlocks |
| **Órdenes inmutables** | El esquema no tiene `status` y `order_items` usa FK RESTRICT. No hay PUT/DELETE de órdenes; la cancelación está propuesta como migración |
| **Snapshots** | `order_items` guarda nombre, categoría y precio; `orders` guarda `coupon_code` y `discount_cap_pct`: el histórico no cambia si cambia el catálogo |
| **Redondeo por paso** | Reproduce el seed al centavo (83.505 → 83.51 antes de sumar) |
| **order_number** | `max(order_number)` del año + 1 (`ORD-2026-000005`), garantizado por `uk_orders_order_number` + reintento acotado (`order.number.max-attempts=3`), cada intento en una transacción nueva |
| **Zona horaria única** | `America/Bogota` (`app.time-zone` del `pom.xml`) para `hibernate.jdbc.time_zone` y `-Duser.timezone` (surefire y `spring-boot:run`): `LocalDateTime.now()` y los `DATETIME` usan la misma zona |
| **`PricingService` y `TransactionTemplate`** | `PricingService` concentra el cálculo (DRY). `OrderService` usa `TransactionTemplate` para abrir una transacción por intento y reintentar fuera de ella |
| **Autenticación con usuarios en BD** | `app_users` con BCrypt en lugar de usuarios en memoria: credenciales fuera del código y usuarios bloqueables (`is_active = 0`) |
| **Validación doble del carrito** | Bean Validation (400 con lista de campos en HTTP) + `CartValidator` (garantía de negocio para cualquier invocador); comparten la detección de duplicados |

## 10. Trade-offs

- `max(order_number)` no es una secuencia: bajo alta concurrencia habrá reintentos. Para producción: tabla de secuencias o `AUTO_INCREMENT` del id como sufijo (requiere migración).
- La verificación previa de stock en la cotización es informativa; la autoritativa es el UPDATE atómico del checkout.
- `/cart/quote` no reserva stock (el carrito vive en el frontend).
- MySQL 5.7 está fuera de soporte de Oracle y obliga a quedarse en Hibernate 6.4 / Boot 3.2 (también sin soporte OSS). Plan recomendado: migrar a MySQL 8.0+ y Boot 3.4+.

## 11. Seguridad

**Implementado (Spring Security + JWT HS256, jjwt 0.12.6):**

- API sin estado (`SessionCreationPolicy.STATELESS`), sin CSRF, sin form login ni HTTP Basic (`SecurityConfig`).
- `POST /api/v1/auth/login` (`AuthController`) autentica con `AuthenticationManager` + `DaoAuthenticationProvider` contra `app_users` (`DatabaseUserDetailsService`, BCrypt). Devuelve `TokenResponse` (`accessToken`, `tokenType = "Bearer"`, `expiresIn` en segundos). Credenciales inválidas o usuario inactivo → 401.
- Rutas públicas: el login, Swagger/OpenAPI (`/v3/api-docs/**`, `/swagger-ui/**`), `/error` y `OPTIONS`. Todo lo demás exige `Authorization: Bearer <token>` (`JwtAuthenticationFilter`).
- 401 (sin token, token inválido o expirado) y 403 en formato Problem Details (`JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, `SecurityProblems`).
- Secreto y vigencia: `app.jwt.secret` y `app.jwt.expiration-ms`, sobrescribibles con `JWT_SECRET` y `JWT_EXPIRATION_MS`.
- CORS en `SecurityConfig.corsConfigurationSource`, con orígenes desde `app.cors.allowed-origins` (definido en los perfiles `dev` y `test`).
- Swagger UI muestra el botón "Authorize" (`OpenApiConfig`, esquema `bearerAuth`).
- Se mantienen: errores sin stack trace ni mensajes internos (`server.error.include-*=never`) y DTO sin campos sensibles ni montos enviados por el cliente.

**Pendiente:**

- Autorización por rol: `app_users.role` se carga como rol de Spring, pero ninguna ruta lo exige (solo `authenticated()`).
- Las credenciales de la BD están fijas en `application.properties`, no en variables de entorno. El secreto JWT tiene un valor por defecto que debe sobrescribirse fuera de `dev`.
- El frontend usa credenciales de demostración fijas (`auth.interceptor.ts`).
- HTTPS, rate limiting en `/auth/login`, `/cart/quote` y `/orders`, refresh tokens y auditoría de accesos.
