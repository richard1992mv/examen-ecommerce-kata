# Arquitectura · Backend examen-ecommerce

## 1. Por qué MVC por capas

El dominio es pequeño (5 tablas) y su complejidad está en **una** zona: el cálculo de descuentos. Una arquitectura MVC por capas (Controller → Service → Repository) es suficiente, conocida por cualquier equipo Spring y fácil de evaluar. La parte compleja se aísla en `service/discount/` como **Java puro** (sin Spring ni JPA), con lo que obtenemos el beneficio principal de una arquitectura hexagonal (núcleo testeable sin infraestructura) sin pagar su coste en todo el proyecto.

## 2. Responsabilidad de cada capa

| Paquete | Responsabilidad | No hace |
|---|---|---|
| `controller/` | HTTP: valida la entrada (`@Valid`), delega, arma 201/204 + Location | Lógica de negocio, acceso a repositorios |
| `dto/request`, `dto/response` | Records de entrada/salida. La entrada no tiene `id`, timestamps ni montos (anti asignación masiva) | Exponer entidades |
| `dto/validation/` | Anotaciones Bean Validation reutilizables: `@Percentage`, `@Money`, `@ValidDateRange`, `@UniqueProductIds` | — |
| `service/` + `service/impl/` | Casos de uso, transacciones, orquestación | Conversión DTO/entidad (mappers), cálculo (motor) |
| `service/discount/` | Motor de descuentos: modelo, Strategy, Factory, Engine. **Java puro** | Importar `entity`, `jakarta.persistence` o Spring |
| `adapter/` | Entidades ↔ modelo del motor ↔ DTO/Order | Calcular |
| `mapper/` | Entidad ↔ DTO (manual), mensaje HU4 | Reglas de negocio |
| `entity/` | Mapeo JPA fiel a `01_schema.sql` | Lógica de aplicación |
| `repository/` | Spring Data JPA, Specifications, UPDATE atómico | — |
| `event/` | Observer: eventos, publicador y listeners (solo efectos secundarios) | Modificar cálculo, respuesta o stock |
| `exception/` | Excepciones de negocio + único `GlobalExceptionHandler` (RFC 9457) | — |
| `config/` | CORS, OpenAPI, `DiscountProperties`, `Clock`, beans del motor | — |
| `util/` | `MoneyUtils` (todo el redondeo), `OrderNumberGenerator`, `DateTimeUtils` | — |

Reglas verificadas por `architecture/DiscountPackagePurityTest`: `service/discount` no importa Spring/JPA/Hibernate/entidades, los controllers no importan repositorios y no hay `@Autowired`.

## 3. Modelo de datos (tabla → entidad)

| Tabla | Entidad | Notas de mapeo |
|---|---|---|
| `categories` | `Category` | `discount_pct DECIMAL(5,2) UNSIGNED` → `BigDecimal` |
| `products` | `Product` | `@ManyToOne(LAZY) Category`; `stock INT UNSIGNED` → `Integer` |
| `coupons` | `Coupon` | `is_active TINYINT(1) UNSIGNED` → `boolean` con `@JdbcTypeCode(TINYINT)`; `isValidAt()` inclusivo |
| `orders` | `Order` (entidad JPA `PurchaseOrder`) | `ORDER` es palabra reservada en JPQL; `@ManyToOne(LAZY, optional) Coupon`; `@OneToMany(mappedBy, PERSIST)` + `addItem()`; `currency CHAR(3)` con `@JdbcTypeCode(CHAR)`; sin setters (inmutable) |
| `order_items` | `OrderItem` | `@ManyToOne(LAZY)` a `Order` y `Product`; snapshots `product_name`, `category_name`, `unit_price` |

- IDs `INT UNSIGNED` → `Integer` (`columnDefinition = "INT UNSIGNED"`), `DATETIME` → `LocalDateTime`, `DECIMAL` → `BigDecimal`.
- `created_at/updated_at` de catálogo los gestiona MySQL (`insertable=false, updatable=false`). En `orders.created_at` la aplicación escribe la hora del `Clock` inyectado (determinista en pruebas y coherente con el año del `order_number`).
- `equals/hashCode` por id (compatibles con proxies) y `toString` sin relaciones.
- Anti N+1: `JOIN FETCH` (`findAllWithCategoryByIdIn`, `findAllWithItems`) y `@EntityGraph` (`findAll(Specification, Pageable)`, `findWithItemsById`). El listado de órdenes devuelve un resumen sin ítems.

No se crearon tablas ni columnas. Lo que requeriría esquema nuevo está en `docs/migraciones-propuestas.md` (sin aplicar).

## 4. Reglas de negocio (cascada multiplicativa)

Redondeo **HALF_UP a 2 decimales al final de cada paso**, igual que `02_seed_data.sql`. Todo pasa por `MoneyUtils`.

| Paso | Regla | Clase |
|---|---|---|
| R1 | Por línea: `line_subtotal × categories.discount_pct`, redondeado por línea. `subtotal_1 = original − categoría` | `CategoryDiscountRule` (order 1) |
| R2 | Si `subtotal_1 > 100.00` (estricto): `subtotal_1 × 5 %` | `VolumeDiscountRule` (order 2) |
| R3 | Cupón válido (existe, activo, `valid_from ≤ ahora ≤ valid_until`): `subtotal_2 × coupons.discount_pct` | `CouponDiscountRule` (order 3) |
| R4 | `bruto = R1+R2+R3`; `tope = original × 35 %`; si `bruto > tope` → ajuste `bruto − tope`, `cap_applied = 1` | `PercentageCapPolicy` |

`total = original − discount_total`; `effective_discount_pct = discount_total / original × 100`; `discount_cap_pct` = snapshot del tope. Porcentajes y umbral en `discount.volume.pct`, `discount.volume.threshold`, `discount.cap-pct`.

Ejemplos del seed (verificados al centavo por `DiscountEngineSeedCasesTest` y por las pruebas de integración):

| Caso | Carrito | Subtotal | Cat. | Vol. | Cupón | Ajuste | Dto. | Total | % ef. |
|---|---|---|---|---|---|---|---|---|---|
| (a) | Novela×2 + Mouse×1 | 50.00 | 2.00 | 0.00 | 0.00 | 0.00 | 2.00 | 48.00 | 4.00 |
| (b) | Teclado×1 + Lámpara×1 | 110.00 | 8.00 | 5.10 | 0.00 | 0.00 | 13.10 | 96.90 | 11.91 |
| (c) | Audífonos×2 + Libro prog.×1 + WELCOME2026 | 130.00 | 9.00 | 6.05 | 17.24 | 0.00 | 32.29 | 97.71 | 24.84 |
| (d) | Monitor + Smartwatch + Balón×2 + TESTCAP30 | 320.00 | 27.00 | 14.65 | 83.51 | 13.16 | 112.00 | 208.00 | 35.00 (HU4) |

Caso (d) paso a paso: 293.00 × 5 % = 14.65 → 278.35 × 30 % = 83.505 → **83.51** (HALF_UP) → bruto 125.16 > 112.00 → ajuste 13.16.

HU4: con `cap_applied = 1` la respuesta incluye `"¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)"`, armado por `DiscountMessageMapper` a partir de `capApplied` (determinista, nunca en un listener).

## 5. Flujos

```
POST /cart/quote                         POST /orders  (@Transactional por intento)
 Controller (@Valid)                      Controller (@Valid)
  └─ CartService                           └─ OrderService ── reintento acotado (UNIQUE order_number)
      └─ PricingService  ◄──── mismo ────────── OrderCheckout.placeOrder
          1 CartValidator                        1-4 PricingService (idéntico a quote)
          2 ProductRepository (1 consulta)       5   StockService.decrement → UPDATE ... WHERE stock >= :qty
          3 CouponValidator (Clock)              6   OrderNumberGenerator (ORD-2026-000005)
          4 StockService.assertAvailable         7   DiscountResultAdapter.toOrder → saveAndFlush
          5 DiscountContextAdapter               8   CartEventPublisher → OrderConfirmedEvent (AFTER_COMMIT)
          6 DiscountRuleFactory                  9   OrderMapper → 201 + Location
          7 DiscountEngine
      └─ DiscountResultAdapter.toQuoteResponse
      └─ CartEventPublisher (CartQuoted / DiscountCapReached)
```

## 6. SOLID y DRY

| Principio | Clase(s) | Cómo se cumple |
|---|---|---|
| **S** Responsabilidad única | `CategoryDiscountRule`, `VolumeDiscountRule`, `CouponDiscountRule`, `DiscountEngine`, `PercentageCapPolicy`, `CouponValidatorImpl`, `CartValidatorImpl`, `StockServiceImpl`, mappers/adapters | Una regla = una clase; el motor solo orquesta; el tope solo limita; validadores y stock separados; controllers sin lógica; mappers/adapters solo convierten |
| **O** Abierto/cerrado | `DiscountRule`, `DiscountRuleFactory`, `DiscountEngine` | Nueva regla = constante en `DiscountType` + clase `DiscountRule` + caso en la Factory (o registro como extensión). El orden lo define `order()`. Probado en `OpenClosedPrincipleTest` |
| **L** Liskov | Todas las `DiscountRule` | Monto ≥ 0, 0 si no aplica, sin excepciones, sin mutar el contexto (records inmutables). Probado en `DiscountRulesTest.CommonContract` |
| **I** Segregación | `OrderService` (escritura) / `OrderQueryService` (lectura), `CouponService` / `CouponValidator`, `CartValidator`, `StockService`, `PricingService`, `OrderCheckout` | Interfaces pequeñas por caso de uso; no existe un `EcommerceService` |
| **D** Inversión | Todos los `*ServiceImpl`, `DiscountEngineConfig`, `ClockConfig` | Inyección por constructor, dependencias por interfaz, `Clock` y `DiscountProperties` inyectados |
| **DRY** dinero | `MoneyUtils` | Único lugar con `setScale/RoundingMode` (`percentOf`, `ratioAsPercent`, …) |
| **DRY** cálculo | `PricingService` | `/cart/quote` y `/orders` ejecutan exactamente el mismo flujo |
| **DRY** errores | `GlobalExceptionHandler`, `ErrorMessages` | Problem Details se construye en un solo lugar; mensajes como constantes |
| **DRY** validación | `@Percentage`, `@Money`, `@ValidDateRange`, `@UniqueProductIds` | Reutilizadas en DTO y `DiscountProperties`; `CartValidatorImpl` reutiliza `UniqueProductIdsValidator.firstDuplicate` |
| **DRY** HTTP | `controller/Responses` | 201 + Location y 204 en un único helper |

## 7. Patrones de diseño

| Patrón | Problema que resuelve | Clases | Alternativa descartada y por qué |
|---|---|---|---|
| **Strategy** | Cada descuento tiene su fórmula y condiciones; deben combinarse en cascada | `DiscountRule` + 3 implementaciones; `DiscountCapPolicy` + `PercentageCapPolicy` | `if/switch` por tipo en un servicio: viola OCP y mezcla responsabilidades |
| **Factory** | Crear las reglas correctas (con/sin cupón) con parámetros de configuración | `DiscountRuleFactory` (único `new` de reglas) | Beans `@Component` por regla + `List<DiscountRule>` inyectada: la regla de cupón depende del cupón de cada petición, no es un singleton |
| **Adapter** | Aislar el motor del modelo JPA | `DiscountContextAdapter`, `DiscountResultAdapter` | Pasar entidades al motor: acopla el cálculo a Hibernate (proxies LAZY, cambios de esquema) y exige BD para probar |
| **Observer** | Auditoría y métricas sin acoplar los servicios | `CartEventPublisher`, `CartEvent` (sealed) + 3 records, `CartAuditListener`, `DiscountMetricsListener` | Llamar logger/métricas desde los servicios: viola SRP; usar listeners para descontar stock: rompe la atomicidad del checkout |

Beneficio del Adapter: el motor se prueba sin BD ni Spring (`DiscountEngineSeedCasesTest`) y un cambio de esquema solo toca el adapter.

## 8. `ddl-auto=validate` con columnas UNSIGNED

**Causa.** `SchemaValidator` compara el tipo JDBC/nombre que reporta Connector/J con el que Hibernate espera para el atributo Java. MySQL reporta `INT UNSIGNED`, `DECIMAL UNSIGNED`, `TINYINT UNSIGNED` (o `BIT` si `tinyInt1isBit=true`) y `CHAR`, mientras Hibernate espera `integer`, `decimal`, `bit` (boolean) y `varchar` (String). Los decimales son equivalentes (`NUMERIC/DECIMAL`), pero boolean, `CHAR(3)` y potencialmente los enteros sin signo pueden fallar.

**Solución aplicada (sin cambiar a `update`):**

1. `columnDefinition = "INT UNSIGNED"` en ids, FKs, `stock` y `quantity`: el validador acepta la columna si su nombre de tipo empieza igual.
2. `boolean` con `@JdbcTypeCode(SqlTypes.TINYINT)` + `tinyInt1isBit=false` en la URL JDBC: ambos lados son `TINYINT`.
3. `currency` con `@JdbcTypeCode(SqlTypes.CHAR)`.

`RepositoryIT` arranca con `validate` contra MySQL 5.7.44 real; si arranca, la validación pasó.

**Dialecto.** Spring Boot 3.2.12 trae Hibernate ORM 6.4.x, cuyo `MySQLDialect` todavía soporta MySQL 5.7. Hibernate 6.6+ (Boot 3.4+) sube el mínimo a MySQL 8.0, por eso no se sube de versión.

## 9. Decisiones

| Decisión | Motivo |
|---|---|
| **UPDATE atómico de stock** (`... SET stock = stock - :qty WHERE id = :id AND stock >= :qty`) | Evita sobreventa sin bloqueos optimistas ni lecturas previas; 0 filas → `InsufficientStockException` → rollback total. Se ejecuta en orden de `productId` para minimizar deadlocks |
| **Órdenes inmutables** | El esquema no tiene `status` y `order_items` usa FK RESTRICT. No hay PUT/DELETE de órdenes; la cancelación está propuesta como migración |
| **Snapshots** | `order_items` guarda nombre, categoría y precio; `orders` guarda `coupon_code` y `discount_cap_pct`: el histórico no cambia si cambia el catálogo |
| **Redondeo por paso** | Reproduce el seed al centavo (83.505 → 83.51 antes de sumar) |
| **Observer sin efectos en el cálculo** | Los listeners solo registran/contabilizan; un fallo se captura y registra. `OrderConfirmedEvent` se procesa `AFTER_COMMIT`: un checkout con rollback nunca notifica |
| **order_number** | `max(order_number)` del año + 1 (`ORD-2026-000005`), garantizado por `uk_orders_order_number` + reintento acotado (`order.number.max-attempts=3`), cada intento en una transacción nueva |
| **Zona horaria única** | `America/Bogota` para `Clock`, `hibernate.jdbc.time_zone`, `connectionTimeZone` y `-Duser.timezone` (Maven): sin desfases en `DATETIME` |
| **Factory como `@Bean`** | El prompt pide `@Component`, pero también que `service/discount` sea Java puro. Se registra en `config/DiscountEngineConfig` con los valores de `DiscountProperties`: mismas garantías, sin imports de Spring en el motor |
| **`PricingService` y `OrderCheckout`** | Servicios internos adicionales para cumplir DRY (cálculo único) y permitir el reintento fuera de la transacción |
| **Validación doble del carrito** | Bean Validation (400 con lista de campos en HTTP) + `CartValidator` (garantía de negocio para cualquier invocador); comparten la detección de duplicados |

## 10. Trade-offs

- `max(order_number)` no es una secuencia: bajo alta concurrencia habrá reintentos. Para producción: tabla de secuencias o `AUTO_INCREMENT` del id como sufijo (requiere migración).
- La verificación previa de stock en la cotización es informativa; la autoritativa es el UPDATE atómico del checkout.
- `/cart/quote` no reserva stock (el carrito vive en el frontend).
- MySQL 5.7 está fuera de soporte de Oracle y obliga a quedarse en Hibernate 6.4 / Boot 3.2 (también sin soporte OSS). Plan recomendado: migrar a MySQL 8.0+ y Boot 3.4+.

## 11. Seguridad pendiente

Fuera de alcance en esta fase: sin Spring Security, sin JWT/OAuth2, sin rate limiting. Se cubre: CORS por perfil, credenciales solo por variables de entorno, errores sin stack trace ni SQL, DTO sin campos sensibles ni montos de cliente, logs de auditoría sin datos personales. Próximos pasos: OAuth2 Resource Server (JWT), roles (`ADMIN` para catálogo/cupones, `CUSTOMER` para checkout), HTTPS, rate limiting en `/cart/quote` y `/orders`, y auditoría de cambios de catálogo.
