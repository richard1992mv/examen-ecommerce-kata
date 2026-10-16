# Arquitectura · examen-ecommerce

Este documento responde primero las tres preguntas obligatorias de la kata (A, B y C) y después detalla la arquitectura del backend (§1–§11).

## Preguntas obligatorias de la kata

### A. ¿Por qué se seleccionó este stack tecnológico y este diseño de carpetas?

El problema es un checkout con **dinero exacto**, **reglas de descuento en cascada configurables**, **stock que no puede quedar negativo** y una **interfaz que se recalcula en tiempo real**. Cada elección del stack responde a una de esas necesidades:

| Capa | Elección | Por qué para este problema |
|---|---|---|
| Lenguaje backend | **Java 17** (LTS) | Tipado estático fuerte (requisito de tipado estricto), `BigDecimal` nativo para montos sin errores de coma flotante y `record` para DTO y resultados inmutables (`DiscountLine`, `DiscountResult`, `CartQuoteResponse`) |
| Framework | **Spring Boot 3.2.12** | Resuelve con piezas estándar lo que la kata exige sin código propio: validación declarativa (`@Valid`), transacciones (`TransactionTemplate`), inyección por constructor (SOLID/DIP), configuración tipada y validada (`@ConfigurationProperties` → `DiscountProperties`, para no fijar el 5 %, el umbral ni el tope en el código), seguridad y Problem Details (RFC 9457). Se fija la línea 3.2 porque es la última cuyo Hibernate (6.4) soporta MySQL 5.7 (§8) |
| Base de datos | **MySQL 5.7.44** + Spring Data JPA | Era el motor del entorno definido para la prueba, sobre el que se diseñó el esquema (`01_schema.sql`). Un motor relacional es lo adecuado aquí: `DECIMAL(12,2)` para dinero, FK `RESTRICT` para el histórico y transacciones ACID para que descontar stock y guardar la orden sea todo o nada. JPA se usa con `ddl-auto=validate`: el esquema lo gobiernan los scripts SQL y Hibernate solo comprueba que el mapeo coincida |
| Seguridad | **JWT HS256 sin estado** (jjwt) + BCrypt | El frontend es una SPA separada: un token sin sesión en servidor encaja con una API REST y escala sin afinidad de sesión. Los usuarios viven en `app_users` con hash BCrypt |
| Contrato de la API | **springdoc-openapi** (Swagger UI) + colección Postman | El contrato se documenta solo a partir de los controllers y las HU1–HU4 se pueden probar de punta a punta con JWT |
| Frontend | **Angular 22** standalone + **signals**, TypeScript estricto | El carrito es estado reactivo: los signals recalculan el subtotal al instante (HU1) y disparan la recotización al backend (HU2/HU4) sin una librería de estado extra. Los componentes standalone evitan los `NgModule`. TypeScript con `strict` + `strictTemplates` cumple el tipado estricto también en las plantillas |
| UI | Bootstrap 5.3 (solo CSS) + Font Awesome 7 | Maquetación accesible y consistente sin escribir CSS propio ni cargar el JS de Bootstrap. Los íconos se importan uno por uno (tree-shaking) |
| Pruebas | JUnit 5, MockMvc (`@WebMvcTest`), `@SpringBootTest`, Vitest | Unitarias puras para el motor (milisegundos, sin Spring ni BD), MockMvc para seguridad y `@SpringBootTest` contra MySQL para los flujos completos con el seed |

**Diseño de carpetas.**

- **Monorepo** (`apps/backend`, `apps/frontend`, `docs/`, `postman/`): un solo repositorio para evaluar, y el contrato backend ↔ frontend cambia en el mismo commit en ambos lados. Los scripts SQL viven en `docs/db_kata_ecommerce/` como fuente única del esquema.
- **Backend por capas técnicas** (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`, `exception/`, `config/`, `security/`, `util/`): hay un solo contexto de negocio (el checkout), y las cuatro HU comparten las mismas entidades y el mismo cálculo. Empaquetar por funcionalidad habría producido un único paquete; por capas, cada carpeta tiene una responsabilidad reconocible para cualquier equipo Spring (§2).
- **Excepción deliberada: `service/discount/`.** La complejidad del problema está concentrada en el motor de descuentos, así que tiene su propio subpaquete, aislado del resto (pregunta C).
- **Frontend** con `models/` (interfaces que reflejan los DTO del backend), `services/` (estado y HTTP) y `components/` (presentación).

### B. ¿Qué trade-offs de arquitectura se asumieron?

**Contexto que guía las decisiones.** La KATA es una prueba técnica con tiempo acotado: un MVP de checkout (HU1–HU4) que debe ser **correcto al centavo**, **evaluable por un tercero en pocos minutos** y **defendible leyendo el código**. Con ese marco, los atributos de calidad se priorizaron en este orden:

1. **Exactitud del dinero y de las reglas.** Un centavo de diferencia invalida el ejercicio y, en un negocio real, genera reclamos y descuadres contables.
2. **Integridad del inventario y de las órdenes.** No vender lo que no hay ni duplicar órdenes.
3. **Velocidad de entrega y facilidad de evaluación.** Leer, ejecutar y probar rápido.
4. **Extensibilidad, solo donde el negocio va a pedir cambios:** las reglas de descuento.
5. **Escalabilidad y operación**, diferidas a propósito: el volumen de una KATA no las exige.

El criterio que resume todos los trade-offs: **se paga complejidad solo donde el negocio genera variación o riesgo; en el resto se elige la opción más simple y se documenta el camino de salida.**

#### B.1 Simplicidad vs. extensibilidad

- **Decisión:** monolito por capas con clases concretas, sin interfaces por servicio. La única abstracción de extensión está en el motor: Strategy + Factory (§7).
- **Visión de negocio:** en un e-commerce, lo que más cambia son las promociones (campañas, temporadas, alianzas). Con una regla por clase, lanzar una promoción nueva es agregar una estrategia y registrarla en la fábrica, sin tocar ni volver a probar las reglas existentes. Eso acorta el *time-to-market* de marketing. Catálogo y órdenes son estables dentro del alcance y no justifican esa inversión.
- **Visión técnica:** menos capas y archivos para el evaluador. Los patrones Adapter y Observer y las interfaces por servicio de la primera versión se eliminaron porque no tenían una segunda implementación: eran sobreingeniería (`ia.md`).
- **Riesgo asumido:** si aparece una segunda implementación real (otro motor de precios, otro proveedor), habrá que extraer una interfaz. El costo es bajo: la inyección por constructor ya está en todas las clases.
- **Cuándo revisarla:** al aparecer esa segunda implementación. Separar en módulos o servicios solo si hay equipos que despliegan de forma independiente.

#### B.2 Exactitud vs. rendimiento de cálculo

- **Decisión:** `BigDecimal` con redondeo HALF_UP a 2 decimales en **cada** paso de la cascada multiplicativa.
- **Visión de negocio:** lo que el cliente ve en el carrito es exactamente lo que se cobra y lo que registra contabilidad. Redondear por paso reproduce el seed (83.505 → 83.51) y hace el desglose auditable línea por línea ante un reclamo.
- **Visión técnica:** `BigDecimal` es más lento y verboso que `double`, pero un carrito tiene pocas líneas y el cálculo es O(n) en memoria (microsegundos); el costo real de una petición está en la red y en la BD. La verbosidad se contiene centralizando toda la aritmética en `MoneyUtils`.
- **Riesgo asumido:** ninguno relevante.
- **Cuándo revisarla:** no se revisa: con dinero, la exactitud no se negocia por rendimiento.

#### B.3 Backend como única fuente de verdad vs. latencia en la interfaz

- **Decisión:** el frontend solo calcula el subtotal original. Todos los descuentos llegan de `POST /cart/quote`, que relee precios, porcentajes y stock de la BD en cada llamada. Cotización y checkout comparten `PricingService.price()`.
- **Visión de negocio:** nadie puede manipular un precio o un porcentaje desde el navegador (prevención de fraude). Además, la promoción que configura el negocio es la misma que se cobra y la cotización (HU2) siempre coincide con la orden (HU3), así que no hay reclamos de "me mostraron otro total".
- **Visión técnica:** cada cambio del carrito cuesta un viaje de red y una consulta (`JOIN FETCH`). Se mitiga en el frontend con un debounce de 300 ms y `switchMap`, que cancela cotizaciones obsoletas. A cambio, las reglas no se duplican en TypeScript y Java (DRY de todo el stack).
- **Riesgo asumido:** si el backend no responde, el usuario ve el subtotal pero no el desglose (degradación controlada, con mensaje de error).
- **Cuándo revisarla:** con tráfico alto, cachear el catálogo con invalidación. Las reglas nunca se mueven al cliente.

#### B.4 Consistencia vs. throughput en el checkout

- **Decisión:** descuento de stock con un `UPDATE ... WHERE stock >= :qty` atómico, una transacción por intento y número de orden con `max()` + restricción UNIQUE y reintento acotado.
- **Visión de negocio:** cero sobreventa y cero órdenes duplicadas. Vender algo que no hay cuesta cancelaciones, devoluciones y reputación; es preferible rechazar a tiempo con un 409 claro que confirmar una orden imposible de cumplir.
- **Visión técnica:** no hay bloqueos pesimistas, tablas extra ni cambios al esquema dado. El costo aparece bajo alta concurrencia: reintentos del número de orden y contención en las filas de los productos más vendidos (§10).
- **Riesgo asumido:** en un pico de ventas (por ejemplo, un *Black Friday*) habría más reintentos y algunos 409 adicionales.
- **Cuándo revisarla:** antes de una carga real, con una tabla de secuencias (migración M2) y, si el negocio lo pide, una reserva de stock al agregar al carrito.

#### B.5 Velocidad de entrega vs. automatización y operación

- **Decisión:**
  - el esquema se gobierna con scripts SQL, sin Flyway;
  - las pruebas con BD corren contra el MySQL local, sin Testcontainers;
  - las credenciales de la BD están en `application.properties`;
  - el frontend usa un login de demostración.
- **Visión de negocio:** el evaluador ejecuta tres scripts, levanta las dos aplicaciones y prueba. El tiempo del ejercicio se invirtió en lo que se evalúa (reglas, exactitud, integridad), no en infraestructura que una KATA no necesita.
- **Visión técnica:** se asume deuda técnica conocida y acotada:
  - migraciones manuales;
  - pruebas que dependen de una BD local y de la fecha real (fallarán desde el 2027-01-01, cuando vence WELCOME2026);
  - credenciales fijas y sin rate limiting.
- **Lo que no se cedió:** la calidad verificable. La cobertura mínima del 80 % sí es una puerta automática del build en ambos lados: JaCoCo en `mvn clean verify` (líneas, instrucciones y ramas) y `coverageThresholds` en `npm run test:coverage`. Un cambio que la baje no compila en verde.
- **Riesgo asumido:** no es apta para CI ni producción tal cual. Para que la deuda sea visible, cada punto está documentado con su solución (`migraciones-propuestas.md`, §10, §11).
- **Cuándo revisarla:** antes de integrar en CI o desplegar, con Flyway, Testcontainers con un `Clock` fijo, variables de entorno y un login real.

#### B.6 Configuración estática vs. administración en caliente

- **Decisión:** el % de volumen, el umbral y el tope van en `application.properties` y se validan al arrancar (`DiscountProperties`). Los % de categoría y de cupón van en la BD.
- **Visión de negocio:** cambiar un porcentaje no requiere desarrollo. La separación sigue al dueño de cada dato:
  - el umbral y el tope son **política comercial estable** (decisión de dirección), por eso van en configuración;
  - los cupones son **operación diaria** (se crean y vencen por campaña), por eso van en la BD, con vigencia y estado.
- **Visión técnica:** cambiar la configuración exige reiniciar o volver a desplegar, y no hay una pantalla de administración. A cambio, un valor inválido (por ejemplo, un tope del 150 %) impide arrancar en lugar de cobrar mal.
- **Cuándo revisarla:** si marketing necesita autonomía para cambiar reglas sin despliegues, con una tabla de reglas y un backoffice.

#### B.7 Compatibilidad vs. actualización tecnológica

- **Decisión:** respetar MySQL 5.7.44 del entorno dado, lo que fija Spring Boot 3.2 / Hibernate 6.4 (§8).
- **Visión de negocio:** se trabaja sobre la infraestructura existente, sin sumar una migración de datos al alcance del MVP.
- **Visión técnica:** las tres versiones están fuera de soporte (riesgo de seguridad y de parches), y la dependencia del framework queda bloqueada.
- **Cuándo revisarla:** es el primer paso hacia producción: MySQL 8.0+ y Boot 3.4+.

#### Mapa de decisiones

| Trade-off | Se priorizó | Se cedió | Señal para revisarlo |
|---|---|---|---|
| B.1 Simplicidad vs. extensibilidad | Código simple; extensión solo en promociones | Interfaces en el resto de servicios | Una segunda implementación real |
| B.2 Exactitud vs. rendimiento | Dinero exacto y auditable | Microsegundos de CPU | Nunca |
| B.3 Fuente única de verdad vs. latencia | Seguridad de precios y coherencia cotización = orden | Un viaje de red por cambio | Tráfico alto (caché de catálogo) |
| B.4 Consistencia vs. throughput | Cero sobreventa y cero duplicados | Reintentos bajo picos | Carga real (secuencias, reservas) |
| B.5 Entrega vs. automatización | Tiempo en las reglas de negocio, con cobertura ≥ 80 % verificada | CI, migraciones automáticas, aislamiento de BD en pruebas | Integrar en CI o desplegar |
| B.6 Configuración vs. administración | Validación al arrancar, sin desarrollo | Cambios sin reiniciar | Marketing pide autonomía |
| B.7 Compatibilidad vs. actualización | Entorno dado, sin migrar datos | Soporte y parches | Primer paso a producción |

**Conclusión del arquitecto:** ningún trade-off compromete la corrección de las HU1–HU4 (exactitud, cascada, tope e integridad del stock). Todo lo que se cedió es escala, operación o automatización: aspectos reversibles, documentados y con un disparador claro. La arquitectura entrega un MVP correcto hoy y deja definido el camino para llevarlo a producción.

### C. ¿Cómo se aislaron las reglas matemáticas del motor de descuentos de la persistencia y de los controladores?

El motor (`service/discount/`) es una **función pura sobre datos propios**: recibe líneas y un porcentaje de cupón, y devuelve un desglose. No conoce HTTP, JPA ni la base de datos.

```
HTTP (JSON)                    Orquestación y traducción                  Motor (cálculo puro)
──────────────                 ─────────────────────────                  ────────────────────
CartController / OrderController
  CartRequest (DTO) ──► PricingService.price()
                          1. CartValidator: estructura del carrito
                          2. ProductRepository: entidades Product (1 consulta)
                          3. CouponValidator: existe, activo y vigente (Clock)
                          4. StockService: stock suficiente
                          5. Product ──toLine()──► DiscountLine (record) ──► DiscountCalculator.calculate(
                             Coupon ──► Optional<BigDecimal> (solo el %)        lines, couponPct)
                                                                                  └─ reglas Strategy creadas por
                                                                                     DiscountRuleFactory + tope
                          ◄──────────────────────────────────── DiscountResult (record) ◄─┘
  CheckoutMapper: DiscountResult ──► CartQuoteResponse (HU2/HU4) o entidad Order (HU3)
```

Mecanismos concretos:

1. **Modelo propio de entrada y salida.** El motor trabaja con `DiscountLine` (id, nombre, categoría, precio unitario, cantidad, % de categoría) y devuelve `DiscountResult`. Nunca recibe una entidad (`Product`, `Coupon`) ni un DTO HTTP (`CartRequest`). Los únicos imports del paquete son `java.*`, `MoneyUtils`, `DiscountProperties` y la anotación `@Component`: ni `entity`, ni `repository`, ni `jakarta.persistence`, ni `org.springframework.web`.
2. **Una frontera de traducción explícita.** `PricingService` es el único que convierte entidades en `DiscountLine` (`toLine`), y `CheckoutMapper` es el único que convierte `DiscountResult` en respuesta HTTP o en la entidad `Order`. Si cambia el esquema o el JSON, se ajustan esas dos clases y el motor no se toca.
3. **Lo que depende del mundo exterior se resuelve antes.** La vigencia del cupón depende de la fecha y de la BD, así que la valida `CouponValidator` antes de calcular; el motor solo recibe el porcentaje ya validado. El stock tampoco entra al motor. Por eso el cálculo es determinista: mismas líneas y mismo porcentaje dan siempre el mismo resultado.
4. **Parámetros inyectados, no fijos.** Volumen, umbral y tope llegan como `DiscountProperties` (configuración tipada y validada al arrancar). Toda la aritmética monetaria pasa por `MoneyUtils` (HALF_UP a 2 decimales).
5. **Cada regla, aislada también de las demás.** Categoría, volumen y cupón son estrategias independientes (`DiscountRule`) que crea `DiscountRuleFactory` en el orden de la cascada; el tope se aplica al final en `DiscountCalculator` (§7).

**Evidencia de que el aislamiento funciona:**
- `CategoryDiscountTest`, `VolumeDiscountTest`, `CouponDiscountTest`, `DiscountCapTest` y `DiscountRuleFactoryTest` prueban las cuatro reglas y los casos del seed con JUnit puro: sin Spring, sin BD y en milisegundos.
- El mismo motor sirve a `/cart/quote` y a `/orders`, así que la cotización (HU2) y la orden persistida (HU3) nunca difieren.
- El frontend tampoco calcula descuentos: muestra lo que devuelve el motor.

**Límite reconocido:** `DiscountCalculator` y `DiscountRuleFactory` llevan `@Component` para la inyección de dependencias; es la única dependencia de Spring del motor, y la lógica no usa nada del framework. No hay una prueba automática (por ejemplo, ArchUnit) que impida que alguien importe una entidad en `service/discount/` en el futuro (§2).

## 1. Por qué MVC por capas

El dominio es pequeño (5 tablas de negocio + `app_users` para autenticación) y su complejidad está en **una** zona: el cálculo de descuentos. Una arquitectura MVC por capas (Controller → Service → Repository) es suficiente, conocida por cualquier equipo Spring y fácil de evaluar. La parte compleja se aísla en `service/discount/` (`DiscountCalculator`), que opera sobre records propios (`DiscountLine` → `DiscountResult`) sin entidades JPA ni repositorios, de modo que el cálculo se prueba sin base de datos. No es Java puro: `DiscountCalculator` es un `@Component` que recibe `DiscountProperties` por constructor. Dentro del motor, cada regla de la cascada es una estrategia (`DiscountRule`) creada por `DiscountRuleFactory` (§7).

> **Estado actual.** Una primera versión generada incluía más paquetes (`adapter/`, `event/`, `service/impl/`, CRUD de catálogo y cupones). El commit `9a8c8a3` redujo el código al alcance de las HU1–HU4. Este documento describe el código vigente; lo que se eliminó o sigue pendiente se indica explícitamente.

## 2. Responsabilidad de cada capa

| Paquete | Responsabilidad | No hace |
|---|---|---|
| `controller/` | HTTP: valida la entrada (`@Valid`) y delega. `AuthController` (login), `ProductController` (`GET /products`), `CartController` (`POST /cart/quote`), `OrderController` (`POST /orders`, 201) | Lógica de negocio, acceso a repositorios |
| `dto/request`, `dto/response` | Records de entrada/salida. La entrada (`CartRequest`, `CartItemRequest`) no tiene `id`, timestamps ni montos (anti asignación masiva) | Exponer entidades |
| `dto/validation/` | `@UniqueProductIds` + `UniqueProductIdsValidator` (sin `productId` repetidos en el carrito) | — |
| `service/` | Casos de uso como clases concretas (sin interfaces): `PricingService`, `OrderService`, `StockService`, `ProductService`, `CartValidator`, `CouponValidator` | Conversión DTO/entidad (mapper), cálculo (motor) |
| `service/discount/` | Motor de descuentos: `DiscountCalculator` (contexto), `DiscountRule` + `CategoryDiscountRule`, `VolumeDiscountRule`, `CouponDiscountRule` (Strategy), `DiscountRuleFactory` (Factory), `DiscountType`, `DiscountLine`, `DiscountResult` | Importar `entity` o `jakarta.persistence` |
| `mapper/` | `CheckoutMapper`: `DiscountResult` → respuesta de quote, entidad `Order` con snapshots y respuesta de orden; mensaje HU4 | Reglas de negocio |
| `entity/` | Mapeo JPA fiel a `01_schema.sql` (bases `IdentifiedEntity` y `AuditableEntity`) | Lógica de aplicación |
| `repository/` | Spring Data JPA, `JOIN FETCH`/`@EntityGraph`, UPDATE atómico de stock | — |
| `exception/` | Excepciones de negocio + único `GlobalExceptionHandler` (RFC 9457); `ProblemDetails.of` construye todos los Problem Details; todos los mensajes (negocio, validación y seguridad) en `ErrorMessages` | — |
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
- `created_at/updated_at` de catálogo y `app_users` los gestiona MySQL (`AuditableEntity`: `insertable=false, updatable=false`). En `orders.created_at` la aplicación escribe `LocalDateTime.now(clock)` (coherente con el año del `order_number`), con el `Clock` inyectado desde `TimeConfig`.
- `equals/hashCode` por id (compatibles con proxies) y `toString` sin relaciones.
- Anti N+1: `JOIN FETCH` (`findAllWithCategoryByIdIn`, usado en quote y checkout) y `@EntityGraph` (`findAllByOrderByIdAsc`, catálogo de HU1).

La tabla `app_users` se añadió para la autenticación JWT (`01_schema.sql` en instalaciones nuevas, `04_auth_users.sql` en bases ya existentes). Las tablas de negocio no cambiaron. Lo que requeriría esquema nuevo está en `docs/migraciones-propuestas.md` (sin aplicar).

## 4. Reglas de negocio (cascada multiplicativa)

Redondeo **HALF_UP a 2 decimales al final de cada paso**, igual que `02_seed_data.sql`. Todo pasa por `MoneyUtils`.

`DiscountCalculator.calculate()` recorre en orden las reglas R1–R3 que entrega `DiscountRuleFactory` (cada una descuenta sobre el subtotal que dejó la anterior) y después aplica R4; la validez del cupón la decide antes `CouponValidator`.

| Paso | Regla | Dónde |
|---|---|---|
| R1 | Por línea: `line_subtotal × categories.discount_pct`, redondeado por línea. `subtotal_1 = original − categoría` | `CategoryDiscountRule` |
| R2 | Si `subtotal_1 > 100.00` (estricto): `subtotal_1 × 5 %` | `VolumeDiscountRule` |
| R3 | Cupón válido (existe, activo, `valid_from ≤ ahora ≤ valid_until`): `subtotal_2 × coupons.discount_pct` | `CouponValidator` + `CouponDiscountRule` |
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
          3 CouponValidator (Clock inyectado)    7   OrderNumberGenerator (ORD-2026-000005)
          4 StockService.assertAvailable         8   CheckoutMapper.toOrder → saveAndFlush
          5 DiscountCalculator                   9   CheckoutMapper.toOrderResponse
      └─ CheckoutMapper.toQuoteResponse
```

Todas las rutas bajo `/api/v1` (salvo `POST /auth/login`) pasan antes por `JwtAuthenticationFilter` (§11).

## 6. SOLID y DRY

| Principio | Clase(s) | Cómo se cumple |
|---|---|---|
| **S** Responsabilidad única | `CartValidator`, `CouponValidator`, `StockService`, `DiscountCalculator`, `CheckoutMapper`, `OrderNumberGenerator` | Validación del carrito, del cupón, stock, cálculo, conversión y numeración en clases separadas; controllers sin lógica |
| **O** Abierto/cerrado | `DiscountRule`, `DiscountRuleFactory`, `DiscountProperties` | Los porcentajes y el umbral se cambian por configuración o en la BD sin tocar código. Una regla nueva es otra implementación de `DiscountRule` registrada en la fábrica (y un valor en `DiscountType`); el bucle de `DiscountCalculator` no cambia. Si la regla necesita su propio campo en el desglose, también cambian `DiscountResult` y la respuesta |
| **L** Liskov | `CategoryDiscountRule`, `VolumeDiscountRule`, `CouponDiscountRule` | Las tres reglas son intercambiables para `DiscountCalculator`: todas cumplen el contrato de `DiscountRule` (reciben el subtotal acumulado y devuelven un monto ≥ 0 redondeado) |
| **I** Segregación | `PricingService`, `StockService`, `CartValidator`, `CouponValidator` | Servicios pequeños por responsabilidad; no existe un `EcommerceService`. No se definen interfaces |
| **D** Inversión | Todas las clases de `service/`, `security/`, `mapper/`; `TimeConfig`; `DiscountCalculator` | Inyección por constructor en todas las clases (sin `@Autowired` en campos). `DiscountCalculator` depende de la abstracción `DiscountRule` y recibe `DiscountRuleFactory` inyectada en lugar de construirla. La hora no se obtiene con `LocalDateTime.now()`: `CouponValidator` y `OrderService` reciben un `Clock` (bean de `TimeConfig`), y las pruebas lo sustituyen por `Clock.fixed(...)` (`CouponValidatorTest`). Los servicios dependen de clases concretas sin interfaces propias, porque cada uno tiene una sola implementación |
| **DRY** dinero | `MoneyUtils` | Único lugar con `setScale/RoundingMode` (`percentOf`, `ratioAsPercent`, …) |
| **DRY** cálculo | `PricingService` | `/cart/quote` y `/orders` ejecutan exactamente el mismo flujo |
| **DRY** errores | `ProblemDetails`, `ErrorMessages` | `ProblemDetails.of(status, tipo, detalle)` es el único lugar que arma un Problem Details; lo usan `GlobalExceptionHandler`, `AuthController` (401 de login) y los manejadores 401/403 de seguridad (`SecurityProblems` solo escribe la respuesta servlet). Todos los mensajes son constantes de `ErrorMessages` |
| **DRY** validación | `@UniqueProductIds` | `CartValidator` reutiliza `UniqueProductIdsValidator.firstDuplicate` |

## 7. Patrones de diseño

El requisito de la kata (al menos dos patrones de diseño) se cubre con **Strategy** y **Factory** en el motor de descuentos (`service/discount/`), implementados con el alcance mínimo: sin beans adicionales, sin reflexión ni registro dinámico.

| Patrón | Problema que resuelve | Clases | Alternativa descartada y por qué |
|---|---|---|---|
| **Strategy** | Cada descuento tiene su propia fórmula y condición, pero todos se combinan igual en la cascada | `DiscountRule` (interfaz: `type()` y `calculate(lines, subtotal)`); `CategoryDiscountRule`, `VolumeDiscountRule`, `CouponDiscountRule`. `DiscountCalculator` es el contexto | `if/switch` por tipo o fórmulas en línea dentro del calculador: viola OCP y mezcla las tres reglas en un método |
| **Factory** | Decidir **qué** reglas aplican (cupón solo si hay uno válido) y **en qué orden** (Categoría → Volumen → Cupón; el orden cambia el resultado) | `DiscountRuleFactory.createRules(couponPct)`: único lugar con `new` de reglas | Beans `@Component` por regla + `List<DiscountRule>` inyectada: la regla de cupón depende del porcentaje de cada petición, no es un singleton, y el orden quedaría implícito |

Cómo encajan en `DiscountCalculator.calculate()`:

```java
for (DiscountRule rule : ruleFactory.createRules(couponPct)) {
    BigDecimal amount = rule.calculate(lines, subtotal);   // Strategy: fórmula de cada regla
    amounts.put(rule.type(), amount);                       // DiscountType → campo del desglose
    subtotal = MoneyUtils.subtract(subtotal, amount);       // cascada multiplicativa
}
```

Decisiones de alcance:

- **El tope (R4) no es una estrategia.** No es un paso de la cascada sino un límite sobre la suma de R1–R3, por eso se resuelve en `DiscountCalculator` (la primera versión tenía además `DiscountCapPolicy`; se descartó por no tener más de una implementación).
- **La fábrica es un bean inyectado.** `DiscountRuleFactory` es un `@Component` que `DiscountCalculator` recibe por constructor (inversión de dependencias: el calculador no decide cómo se construyen sus reglas). Las pruebas unitarias la crean con `new DiscountRuleFactory(properties)`. Las reglas sin estado por petición (categoría y volumen) se crean una vez; la de cupón, en cada cálculo.
- **Las reglas son `final` y visibles solo dentro del paquete**; fuera de `service/discount/` solo se usa `DiscountCalculator`.
- `CategoryDiscountRule.discountFor(line)` también la usa el calculador para el desglose por línea (`LineResult`), de modo que la fórmula de categoría vive en un solo lugar.

Pruebas: `CategoryDiscountTest`, `VolumeDiscountTest`, `CouponDiscountTest` y `DiscountCapTest` verifican cada regla a través del calculador; `DiscountRuleFactoryTest` verifica el orden de las reglas con y sin cupón y que la regla de cupón usa el porcentaje de la petición.

Los patrones **Adapter** (`DiscountContextAdapter`, `DiscountResultAdapter`) y **Observer** (`CartEventPublisher` y listeners) de la primera versión se eliminaron en `9a8c8a3` y no se recuperan: los records `DiscountLine`/`DiscountResult` ya aíslan el motor de JPA, y la auditoría por eventos no la pide ninguna HU.

Otros elementos de diseño presentes en el código:

| Elemento | Clases | Observación |
|---|---|---|
| **Builder** | `Order.builder()` (`Order.Builder`) | Construye la orden inmutable (sin setters) con todos los montos y snapshots |
| **Mapper / DTO** | `CheckoutMapper`, `ProductResponse.from` | Separa entidades y contrato HTTP |
| **Template Method de Spring** | `TransactionTemplate` en `OrderService` | Una transacción nueva por intento de checkout, para poder reintentar fuera de ella |
| **Filtro de cadena (Spring Security)** | `JwtAuthenticationFilter` en `SecurityConfig` | Autenticación por token antes de los controllers |

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
| **Zona horaria única** | `America/Bogota` (`app.time-zone` del `pom.xml`) para `hibernate.jdbc.time_zone` y `-Duser.timezone` (surefire y `spring-boot:run`): el `Clock` de `TimeConfig` (`Clock.systemDefaultZone()`) y los `DATETIME` usan la misma zona |
| **`PricingService` y `TransactionTemplate`** | `PricingService` concentra el cálculo (DRY). `OrderService` usa `TransactionTemplate` para abrir una transacción por intento y reintentar fuera de ella |
| **Autenticación con usuarios en BD** | `app_users` con BCrypt en lugar de usuarios en memoria: credenciales fuera del código y usuarios bloqueables (`is_active = 0`) |
| **Tipado estricto de extremo a extremo** | Backend: DTO como `record` con tipos concretos (`BigDecimal` para dinero, `Integer`/`int`, `boolean`, `List<T>`), sin tipos crudos ni `@SuppressWarnings`; los únicos `Object` vienen de contratos externos (`equals(Object)` y `HttpServletRequest.getAttribute`, este último acotado con `instanceof TokenError`). Frontend: `strict` + `strictTemplates`, sin `any` ni `$any`; interfaces en `models/` que reflejan cada DTO; `unknown` solo para errores no tipados de RxJS, acotado antes de usarse (ver `apps/frontend/README.md`) |
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
- Secreto y vigencia: `app.jwt.secret` y `app.jwt.expiration-ms`, sobrescribibles con `JWT_SECRET` y `JWT_EXPIRATION_MS`. `JwtService` usa el `Clock` inyectado para emitir (`iat`/`exp`) y para validar la expiración (`JwtParserBuilder.clock`), de modo que `SecurityIntegrationTest` simula un token vencido con un reloj desplazado.
- CORS en `SecurityConfig.corsConfigurationSource`, con orígenes desde `app.cors.allowed-origins` (definido en los perfiles `dev` y `test`).
- Swagger UI muestra el botón "Authorize" (`OpenApiConfig`, esquema `bearerAuth`).
- Se mantienen: errores sin stack trace ni mensajes internos (`server.error.include-*=never`) y DTO sin campos sensibles ni montos enviados por el cliente.

**Pendiente:**

- Autorización por rol: `app_users.role` se carga como rol de Spring, pero ninguna ruta lo exige (solo `authenticated()`).
- Las credenciales de la BD están fijas en `application.properties`, no en variables de entorno. El secreto JWT tiene un valor por defecto que debe sobrescribirse fuera de `dev`.
- El frontend usa credenciales de demostración fijas (`auth.interceptor.ts`).
- HTTPS, rate limiting en `/auth/login`, `/cart/quote` y `/orders`, refresh tokens y auditoría de accesos.
