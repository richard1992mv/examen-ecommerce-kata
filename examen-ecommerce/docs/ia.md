# Gobernanza y uso de IA

Este documento registra cómo se usó la IA en la KATA: qué agente se usó y con qué controles (§1), qué prompts se escribieron y qué produjo cada uno (§2), **las dos correcciones de fondo hechas al resultado de la IA** (§3), los errores típicos de la IA detectados y corregidos (§4) y las diferencias entre lo pedido en los prompts y lo entregado (§5). El texto completo de los prompts está en el Anexo.

## 1. Agente utilizado y forma de trabajo

| Elemento | Detalle |
|---|---|
| **Agente** | **Claude Code** (Anthropic): agente de terminal que lee el repositorio, edita archivos, ejecuta compilaciones y pruebas, y hace commits cuando se le pide |
| **Contexto del agente** | `CLAUDE.md` en la raíz: comandos, arquitectura, convenciones (todo en español) y brechas conocidas. Se actualiza en cada cambio relevante para que el agente no trabaje con información vieja |
| **Fuentes de verdad entregadas a la IA** | `01_schema.sql` y `02_seed_data.sql` (esquema y casos de referencia a–d con sus cálculos). El prompt de backend prohíbe inventar tablas o columnas |
| **Responsable** | El desarrollador revisa cada entrega, decide el alcance y aprueba los commits; la IA no publica nada sin instrucción explícita |

**Controles aplicados sobre la IA** (*human in the loop*):

1. **Primero lectura, después cambios.** Antes de modificar, se pidió al agente analizar la estructura y la documentación en modo solo lectura y esperar la indicación de qué archivos tocar.
2. **Restricciones explícitas en cada prompt.** Ejemplos: "no modifiques la lógica de negocio", "el frontend no se toca", "sin sobreingeniería", "todas las pruebas deben seguir pasando".
3. **Verificación ejecutando, no confiando.** Los prompts prohíben afirmar "compila" o "cubre 80 %" sin ejecutarlo. Cada cambio se validó con `mvn clean verify` (JaCoCo ≥ 80 %), `ng build` y `npm run test:coverage`.
4. **Casos de negocio al centavo.** Las pruebas verifican los casos a–d del seed (por ejemplo, el caso (c) `130.00 − 32.29 = 97.71` y el caso (d) con el tope del 35 %). Un error de redondeo o de cascada hace fallar el build.
5. **Commits pequeños y descriptivos**, para poder auditar qué hizo la IA en cada paso (ver la columna "Commits" en §2).

## 2. Prompts utilizados

| # | Prompt (texto completo en el Anexo) | Rol pedido a la IA | Qué produjo | Commits |
|---|---|---|---|---|
| P1 | **Base de datos MySQL 5.7.44** | Arquitecto de bases de datos senior | `00_create_database.sql` a `03_validation_queries.sql`, diccionario de datos, diagrama ER y limitaciones (`docs/db_kata_ecommerce/`). Cinco tablas, DECIMAL UNSIGNED, FK RESTRICT, snapshots y cuatro órdenes de referencia con el cálculo paso a paso en comentarios | `fcb9d9e`, `68fc74b`, `0a5c082` |
| P2 | **Desarrollador backend senior Java** | Backend senior Spring Boot | Primera versión del backend: capas, entidades, motor de descuentos, checkout transaccional, Problem Details y pruebas | `b5259c1` (luego corregido, ver §3) |
| P3 | **Frontend senior Angular** | Frontend senior Angular (standalone, signals, TS estricto) | `apps/frontend`: modelos que reflejan los DTO, `CartService` con signals y recotización, componentes de las HU1–HU4 y pruebas de `CartService` | `4ff0aac`, `3f85c3b` (estilos e íconos) |
| P4 | **Seguridad JWT + Swagger** | Senior Spring Boot 3 | `security/` (JwtService, filtro, entry point 401, SecurityConfig), `AuthController`, botón *Authorize* en Swagger e interceptor en el frontend, sin tocar la lógica de negocio | `2e41d2d`, `9c5feb5`, `084e62a` (Postman). Después, los usuarios pasaron a la tabla `app_users` (`7d77168`, `d4b433d`) |

**Iteraciones posteriores con Claude Code** (instrucciones del desarrollador, en orden):

| Instrucción | Resultado | Commit |
|---|---|---|
| "Reduce el código a las historias de usuario" | Reducción de alcance (corrección 1, §3) | `9a8c8a3` |
| "Implementa Strategy para los descuentos y Factory para instanciar las reglas, sin sobreingeniería, sin cambiar reglas de negocio ni el frontend, con documentación en las clases para explicarlo" | `DiscountRule` + 3 reglas + `DiscountRuleFactory` (corrección 2, §3) | `1dc0c40`, `c9569ff` |
| "Verifica si el código cumple SOLID y DRY" y luego "corrige DRY" y "corrige la D de inversión" | `ProblemDetails.of` único, mensajes centralizados en `ErrorMessages`, `Clock` inyectado y fábrica inyectada | `7a03c8a` |
| "Verifica el tipado estricto de extremo a extremo sin `any` injustificado, haz clean code y actualiza la documentación" | Se eliminaron `$any`, los casts y los `any` implícitos; los modelos de autenticación se tiparon; `JwtService` usa el `Clock` | `7a03c8a` |
| "Agrega un `.gitignore` con buenas prácticas" | `.gitignore` del monorepo; `target/` y los archivos de Eclipse salen del repositorio | `26e0017` |
| "Responde en `arquitectura.md` por qué este stack, qué trade-offs (como arquitecto, en clave de negocio y técnica) y cómo se aisló el motor" | Sección "Preguntas obligatorias de la kata" (A, B y C) | `937697c` |
| "Integra JaCoCo para la cobertura mínima del 80 % en front y back, con comandos claros" | JaCoCo `check` en `mvn verify` y `coverageThresholds` en Angular; más pruebas en ambos lados | `937697c` |

## 3. Las dos correcciones realizadas a la IA

### Corrección 1 · Sobreingeniería: la IA generó mucho más de lo que pedían las historias de usuario

| | Detalle |
|---|---|
| **Qué entregó la IA** (`b5259c1`) | Además de las HU1–HU4: CRUD completo de categorías, productos y cupones; consulta y paginación de órdenes; paquetes `adapter/` y `event/` (Observer con métricas Micrometer); `service/impl/` con una interfaz por servicio; validadores y DTO extra. El commit lo registra como código "sin probar ni verificar las estructuras de las clases" |
| **Por qué estaba mal** | La KATA pide un checkout con cuatro historias. El código extra no aportaba a ninguna HU, multiplicaba lo que había que probar y mantener, y dificultaba la evaluación. El origen fue el propio prompt P2, que pedía endpoints CRUD, Observer y Adapter; la IA lo siguió al pie de la letra sin cuestionar el alcance |
| **Cómo se corrigió** (`9a8c8a3`) | Se pidió a Claude Code limitar el código a las HU1–HU4 y a los casos de negocio. Quedaron 4 endpoints (`/auth/login`, `/products`, `/cart/quote`, `/orders`), servicios concretos sin interfaces y un único camino de cálculo (`PricingService.price`) |
| **Evidencia** | `arquitectura.md` §2 (responsabilidad de cada capa) y §7 (por qué no se recuperaron Adapter y Observer); las pruebas de las HU siguen verificando los casos a–d |
| **Lección** | Un prompt muy detallado no reemplaza el juicio sobre el alcance: hay que revisar cada pieza generada contra las historias de usuario |

### Corrección 2 · Sobrecorrección: al reducir el alcance, la IA eliminó los patrones de diseño obligatorios

| | Detalle |
|---|---|
| **Qué hizo la IA** (`9a8c8a3`) | En la limpieza también borró Strategy, Factory, Adapter y Observer. El motor quedó como un solo método con las cuatro reglas en secuencia. La KATA exige **al menos dos patrones de diseño**, así que el requisito quedó incumplido, y la documentación (README, `arquitectura.md`, `plan.md`) siguió describiendo clases que ya no existían |
| **Por qué estaba mal** | La IA trató "menos código" como objetivo absoluto, sin distinguir la sobreingeniería (CRUD, eventos) de la abstracción que el negocio sí necesita: las reglas de descuento son lo que más cambia en un e-commerce |
| **Cómo se corrigió** (`1dc0c40`, `c9569ff`, `7a03c8a`) | Se reintrodujeron solo los dos patrones con valor real, en su forma mínima: **Strategy** (`DiscountRule` + `CategoryDiscountRule`, `VolumeDiscountRule`, `CouponDiscountRule`) y **Factory** (`DiscountRuleFactory`, que decide qué reglas aplican y en qué orden). El tope del 35 % se dejó en el calculador, porque no es una regla de la cascada. La restricción fue no cambiar reglas de negocio, historias de usuario ni el frontend. Después se reforzó la inversión de dependencias (fábrica y `Clock` inyectados) |
| **Evidencia** | Las pruebas del motor existentes pasaron sin cambiar sus aserciones; se agregó `DiscountRuleFactoryTest`; `arquitectura.md` §7 documenta los patrones; la cobertura del paquete `service.discount` es del 100 % |
| **Lección** | Cada corrección hecha por la IA también se verifica contra **todos** los requisitos, no solo contra la instrucción del momento. Además, la documentación se revisa en el mismo cambio que el código |

## 4. Errores típicos de IA detectados y corregidos

| Error típico | Por qué está mal | Corrección en este proyecto |
|---|---|---|
| **Sumar porcentajes** (5 % + 15 % = 20 % sobre el mismo subtotal) | El negocio es cascada multiplicativa: cada paso se aplica sobre el subtotal anterior. En el caso (c), sumar daría 24.20 en vez de 23.29 | `DiscountCalculator` pasa a cada `DiscountRule` el subtotal que dejó la anterior; prueba `CouponDiscountTest` ("Se calcula sobre el subtotal tras la regla 2, no sobre el original") |
| **Usar `double`** | Errores binarios de punto flotante (0.1 + 0.2 ≠ 0.3) | Solo `BigDecimal`; columnas DECIMAL; `MoneyUtils` |
| **Redondear solo al final** | El seed redondea en cada paso: 83.505 → 83.51 antes de sumar; redondear al final cambia centavos | `percentOf` redondea HALF_UP a 2 decimales en cada regla y en cada línea |
| **Aplicar el tope sobre el total con descuento** | El tope es el 35 % del `subtotal_original` (320 × 35 % = 112.00), no del total ya descontado | `DiscountCalculator`: `cap = percentOf(original, capPct)`; prueba `DiscountCapTest` |
| **`ddl-auto=update`** | Altera un esquema que es fuente de verdad y oculta incompatibilidades | `validate` + `columnDefinition`/`@JdbcTypeCode` (arquitectura §8) |
| **N+1** | Cargar productos o ítems uno a uno multiplica las consultas | `JOIN FETCH` (`findAllWithCategoryByIdIn`) y `@EntityGraph` (`findAllByOrderByIdAsc`). Pendiente: ninguna prueba cuenta las sentencias |
| **Lógica duplicada entre quote y checkout** | Dos cálculos divergen con el tiempo | `PricingService.price` único, reutilizado por `PricingService.quote` y `OrderService` |
| **Descontar stock dentro de un listener** | El listener corre fuera de la transacción (o tras el commit): se pierde la atomicidad y un fallo no hace rollback | `StockService.decrement` con `Propagation.MANDATORY` dentro de la transacción del checkout (hoy no hay listeners) |
| **`LocalDateTime.now()` directo** | Pruebas dependientes de la fecha real (WELCOME2026 vence el 2026-12-31) | Bean `Clock` (`TimeConfig`) inyectado en `CouponValidator`, `OrderService` y `JwtService`; `CouponValidatorTest` fija la fecha y prueba el último segundo de vigencia y el día siguiente |
| **Subir a Spring Boot 3.4/3.5 "por estar más nuevo"** | Hibernate 6.6+ no soporta MySQL 5.7 | Boot 3.2.12, justificado en el `pom.xml` y en `arquitectura.md` §8 |
| **Afirmar "compila y cubre 80 %" sin ejecutarlo** | No es verificable | JaCoCo (backend) y `@vitest/coverage-v8` (frontend) hacen fallar el build por debajo del 80 % |
| **Cobertura engañosa** | Por defecto, v8 solo mide los archivos que importan las pruebas: el frontend mostraba 91 % cuando la cobertura real era 42 % | `coverageInclude` sobre todo `src/app` en `angular.json`; specs para todos los servicios y componentes (99 % real) |
| **Escapes del tipado** (`$any`, `null as unknown`, `any` implícito en callbacks de error) | Rompen el tipado estricto de extremo a extremo que exige la KATA | Referencias de plantilla tipadas, unión discriminada `QuoteOutcome`, `unknown` acotado con guardas de tipo (README del frontend, "Tipado estricto") |
| **Construcción duplicada de errores y mensajes sueltos** | Viola DRY: tres lugares armaban Problem Details y había mensajes fuera de `ErrorMessages` | `ProblemDetails.of` único y todos los mensajes en `ErrorMessages` |
| **Documentación que describe código eliminado** | Tras la reducción de alcance, README y `arquitectura.md` citaban endpoints, clases y patrones inexistentes | Revisión de la documentación contra el código en cada cambio; `CLAUDE.md` indica que, si docs y código difieren, manda el código |

## 5. Diferencias entre lo pedido en los prompts y lo entregado

No todo lo que pedían los prompts quedó en el código. Algunas diferencias son decisiones deliberadas y otras son deuda técnica pendiente:

| Prompt | Pedía | Estado actual | Motivo |
|---|---|---|---|
| P1 | Scripts en `apps/backend/database/` y `docs/base-datos.md` | En `docs/db_kata_ecommerce/` | Fuente única del esquema fuera del backend (reorganización `0db16b9`, `3396785`) |
| P1 | Solo 5 tablas | 5 tablas de negocio + `app_users` | La autenticación JWT con usuarios en BD se agregó después (P4 y `7d77168`) |
| P2 | CRUD de categorías, productos y cupones, consulta de órdenes, Adapter, Observer, `service/impl/` y Actuator | No están | Corrección 1 (§3): no aportan a las HU1–HU4 |
| P2 | Testcontainers con `mysql:5.7.44` y `@DataJpaTest` | Pruebas con BD contra el MySQL local; las dependencias de Testcontainers siguen declaradas en el `pom.xml` | Velocidad de entrega (`arquitectura.md` B.5). Pendiente para CI |
| P2 | Credenciales de BD por variables de entorno | Fijas en `application.properties` | Pendiente (`arquitectura.md` §11) |
| P2 | `Clock` fijo en **todas** las pruebas | Fijo en las unitarias; las pruebas con BD usan el reloj real | Pendiente: fallarán desde el 2027-01-01, cuando vence WELCOME2026 |
| P2 | Cobertura ≥ 80 % con JaCoCo haciendo fallar `mvn verify` | **Cumplido** (líneas 95.5 %, ramas 84.9 %) | — |
| P3 | Sin librerías de UI, CSS mínimo | Bootstrap 5.3 (solo CSS) y Font Awesome | Decisión posterior de estilos (`3f85c3b`); el prompt indicaba que los estilos se trabajarían después |
| P3 | Solo pruebas básicas de `CartService` | Specs para todos los servicios, el interceptor y los componentes (55 pruebas) | Requisito de cobertura del 80 % en el frontend |
| P4 | Usuario en memoria (`app.security.user`) y `application.yml` | Usuarios en la tabla `app_users` con BCrypt; configuración en `application.properties` | Credenciales fuera del código y usuarios bloqueables (`7d77168`) |

## Anexo · Texto completo de los prompts

<details>
<summary><strong>P1 · Base de datos MySQL 5.7.44</strong></summary>

```text
PROMPT: BASE DE DATOS MYSQL 5.7.44 PARA LA KATA "CORE E-COMMERCE CON DESCUENTOS ACUMULATIVOS"

1. Rol y objetivo
Actúa como arquitecto de bases de datos senior experto en MySQL 5.7.44.
Diseña la base de datos mínima que necesita el backend de la KATA para:
- Listar productos pre-configurados (id, nombre, precio unitario, categoría, stock).
- Validar cupones.
- Validar y descontar stock en el checkout.
- Persistir órdenes con el desglose exacto de descuentos.
Alcance: solo scripts SQL, datos de ejemplo y documentación. No generes código de backend ni frontend.

2. Tablas permitidas (solo estas 5; no crees ninguna otra)
- categories: categorías de producto y su porcentaje de descuento de categoría.
- products: catálogo con precio y stock actual.
- coupons: cupones con porcentaje, vigencia y estado.
- orders: cabecera de la orden con el desglose histórico de descuentos.
- order_items: líneas de la orden con valores históricos.
Justificación de las exclusiones:
- No hay tabla de carritos: el carrito vive en el frontend y llega completo en la petición de checkout.
- No hay clientes, almacenes, movimientos de inventario ni tablas de reglas de descuento.
- El umbral de volumen (100.00 USD), el 5 % y el tope del 35 % son constantes del motor en el backend
  y se guardan como snapshot en cada orden.

3. Restricciones técnicas
1. InnoDB, utf8mb4 y utf8mb4_unicode_ci.
2. sql_mode con STRICT_TRANS_TABLES, NO_ZERO_DATE, NO_ZERO_IN_DATE y ERROR_FOR_DIVISION_BY_ZERO.
3. DECIMAL(12,2) para importes y DECIMAL(5,2) para porcentajes. Nunca FLOAT ni DOUBLE.
4. Sin JSON, columnas generadas, CHECK, CTE, funciones de ventana ni otras características de MySQL 8.
5. Sin triggers ni procedimientos almacenados: las reglas las aplica el backend.
6. UNSIGNED en ids, cantidades, stock e importes.
7. Claves foráneas con tipos idénticos y ON DELETE RESTRICT / ON UPDATE RESTRICT. Nunca CASCADE sobre órdenes.
8. snake_case y COMMENT en español en cada tabla y columna.
9. No afirmes que los scripts fueron ejecutados.

4. Diseño de tablas
4.1 categories
id, name (UNIQUE), discount_pct DECIMAL(5,2) DEFAULT 0.00, created_at, updated_at.
"Tecnología" tiene discount_pct = 10.00.

4.2 products
id, name, unit_price DECIMAL(12,2), category_id (FK), stock INT UNSIGNED, created_at, updated_at.
Índice en category_id.

4.3 coupons
id, code (UNIQUE), discount_pct DECIMAL(5,2), valid_from DATETIME, valid_until DATETIME,
is_active TINYINT(1), created_at, updated_at.
Un cupón es válido si existe, is_active = 1 y NOW() está entre valid_from y valid_until.

4.4 orders
id, order_number (UNIQUE), coupon_id (FK NULL), coupon_code (snapshot NULL),
subtotal_original, category_discount_amount, volume_discount_amount, coupon_discount_amount,
cap_adjustment_amount, discount_total, effective_discount_pct, discount_cap_pct DEFAULT 35.00,
cap_applied TINYINT(1), total, currency CHAR(3) DEFAULT 'USD', created_at.

4.5 order_items
id, order_id (FK), product_id (FK), product_name (snapshot), category_name (snapshot),
unit_price (snapshot), quantity INT UNSIGNED, line_subtotal, category_discount_amount, line_total.
UNIQUE(order_id, product_id).

5. Reglas de negocio de la KATA (cascada multiplicativa, en este orden)
Redondeo HALF_UP a 2 decimales al final de cada paso.
1. subtotal_original = suma de unit_price × quantity.
2. Categoría: por cada línea, category_discount_amount = line_subtotal × categories.discount_pct / 100.
   subtotal_1 = subtotal_original − suma de los descuentos de categoría.
3. Volumen: si subtotal_1 > 100.00, volume_discount_amount = subtotal_1 × 5 %; si no, 0.
   subtotal_2 = subtotal_1 − volumen.
4. Cupón: si es válido, coupon_discount_amount = subtotal_2 × coupons.discount_pct / 100.
5. Tope: descuento_bruto = categoría + volumen + cupón.
   Si descuento_bruto > 35 % de subtotal_original:
   - cap_adjustment_amount = descuento_bruto − (subtotal_original × 0.35)
   - cap_applied = 1
6. Cuadre:
   - discount_total = descuento_bruto − cap_adjustment_amount
   - total = subtotal_original − discount_total
   - effective_discount_pct = discount_total / subtotal_original × 100
7. Checkout transaccional, documentado y sin código:
   START TRANSACTION
   → SELECT ... FOR UPDATE de los productos
   → validar stock >= cantidad
   → UPDATE products SET stock = stock − cantidad
   → INSERT orders
   → INSERT order_items
   → COMMIT
   Ante stock insuficiente, cupón inválido o carrito vacío: ROLLBACK y no se crea la orden.
8. Reglas que MySQL 5.7 no garantiza (las valida el backend): quantity > 0, unit_price > 0,
   stock no negativo en concurrencia, vigencia del cupón y los cálculos de la cascada.
9. Observación obligatoria: con solo las 3 reglas de la KATA el descuento máximo es
   1 − 0.90 × 0.95 × 0.85 = 27.325 %, por lo que el tope del 35 % no se alcanza.
   Para demostrar la HU4 se incluye un cupón de prueba con un porcentaje mayor.

6. Datos de ejemplo
- 4 categorías: Tecnología 10 %, y otras 3 con 0 %.
- 12 productos: al menos 5 de Tecnología, 1 con stock 0 y 1 con stock 2.
- Cupones:
  - WELCOME2026: 15 %, activo, vigente hasta 2026-12-31.
  - EXPIRED2025: vencido.
  - INACTIVE10: inactivo.
  - TESTCAP30: 30 %, activo, para demostrar el tope.
- 4 órdenes cuyos importes cuadren exactamente con sus líneas:
  a) subtotal < 100 sin cupón
  b) subtotal > 100 con volumen
  c) con WELCOME2026
  d) con TESTCAP30 y cap_applied = 1
  Incluye en comentarios SQL el cálculo paso a paso de cada orden.

7. Archivos a generar
apps/backend/database/
  00_create_database.sql   (base de datos, charset, sql_mode)
  01_schema.sql            (tablas, PK, FK, UNIQUE e índices)
  02_seed_data.sql         (datos en orden de dependencias)
  03_validation_queries.sql (solo SELECT)
docs/base-datos.md, con:
  - Diccionario de datos
  - Diagrama ER en Mermaid
  - Limitaciones
  - Orden de ejecución

8. Consultas de validación (03)
- Conteos por tabla.
- Productos con stock 0.
- Cupones válidos e inválidos a la fecha actual.
- Recalcular la cascada de cada orden y compararla con los importes guardados.
- effective_discount_pct <= discount_cap_pct.
- line_total y totales de la orden cuadrados.
- Órdenes sin líneas.

9. Entrega
1. Primero, un resumen breve de las decisiones (tablas, exclusiones, supuestos).
2. Luego los archivos completos.
3. Al final, una checklist que separe lo verificado por inspección estática de lo que
   requiere ejecutar en un servidor MySQL 5.7.44 real.
```

</details>

<details>
<summary><strong>P2 · Desarrollador backend senior Java</strong></summary>

```text
ROL: Actúa como desarrollador backend senior Java. Construye apps/backend/ del monorepo
"examen-ecommerce" para la KATA "Core E-Commerce con Sistema de Descuentos Acumulativos".
Fuentes de verdad (adjuntas):
- 01_schema.sql: esquema MySQL existente, 5 tablas (categories, products, coupons, orders,
  order_items).
- 02_seed_data.sql: datos y órdenes de referencia con sus cálculos.
No inventes tablas ni columnas. Si algo requiere cambios, proponlo como migración SQL
documentada, sin aplicarla.

1. STACK
- Java 17, Spring Boot 3.2.x (versión de mantenimiento estable), Maven.
- Spring Web, Spring Data JPA/Hibernate, MySQL Connector/J (com.mysql:mysql-connector-j),
  Bean Validation, Springdoc OpenAPI.
- Spring Boot Actuator: exponer solo /actuator/health y /actuator/metrics.
- Pruebas: JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers (mysql:5.7.44), JaCoCo.
- Solo jakarta.*, nunca javax.*. Sin Lombok.
- MySQL 5.7.44, base kata_ecommerce. Verifica que el dialecto de Hibernate de la versión de Boot
  elegida soporte MySQL 5.7; no subas de versión sin comprobarlo.
- SIN Flyway ni Liquibase. El esquema ya existe:
  · spring.jpa.hibernate.ddl-auto=validate
  · spring.sql.init.mode=never en dev
  · spring.jpa.open-in-view=false
  · hibernate.jdbc.time_zone explícito
  Si validate falla por columnas UNSIGNED, explica la causa y la solución; no cambies a update.
- Credenciales por variables de entorno (DB_URL, DB_USER, DB_PASSWORD), nunca en el código.
- SEGURIDAD: fuera de alcance en esta fase (sin Spring Security ni JWT). Solo CORS configurable
  por perfil. Documéntalo como pendiente.

2. ARQUITECTURA MVC POR CAPAS
com.examen.ecommerce
 ├─ config/                     → CORS, OpenAPI, DiscountProperties (@ConfigurationProperties),
 │                                Clock
 ├─ controller/                 → REST controllers (solo HTTP: validan entrada y delegan)
 ├─ dto/request/, dto/response/ → records (nunca se exponen entidades)
 ├─ entity/                     → Category, Product, Coupon, Order, OrderItem
 ├─ repository/                 → interfaces Spring Data JPA
 ├─ service/, service/impl/     → interfaces e implementaciones
 │                                (CategoryService, ProductService, CouponService, CartService,
 │                                 OrderService, StockService, CouponValidator, CartValidator)
 ├─ service/discount/
 │   ├─ model/                  → DiscountContext, DiscountLine, CouponSnapshot,
 │   │                            DiscountStepResult, CapResult, DiscountResult,
 │   │                            DiscountType (enum)
 │   ├─ strategy/               → DiscountRule, CategoryDiscountRule, VolumeDiscountRule,
 │   │                            CouponDiscountRule, DiscountCapPolicy, PercentageCapPolicy
 │   ├─ factory/                → DiscountRuleFactory
 │   └─ DiscountEngine
 ├─ adapter/                    → DiscountContextAdapter, DiscountResultAdapter
 ├─ event/                      → CartEvent (sealed), CartQuotedEvent, DiscountCapReachedEvent,
 │   │                            OrderConfirmedEvent, CartEventPublisher
 │   └─ listener/               → CartAuditListener, DiscountMetricsListener
 ├─ mapper/                     → conversión entidad ↔ DTO (manual)
 ├─ exception/                  → excepciones de negocio + GlobalExceptionHandler
 │                                (@RestControllerAdvice)
 └─ util/                       → OrderNumberGenerator, MoneyUtils
Reglas de capas:
- Controller → Service → Repository. El controller nunca accede a repositorios.
- service/discount/ es Java puro: no importa entity, jakarta.persistence ni Spring.

3. PRINCIPIOS SOLID Y DRY (obligatorios y verificables)
S - Responsabilidad única:
  · Cada regla de descuento es una clase.
  · DiscountEngine solo orquesta la cadena; DiscountCapPolicy solo aplica el tope.
  · CouponValidator valida cupones, CartValidator valida el carrito y StockService gestiona
    el stock.
  · Los controllers no contienen lógica de negocio; los mappers y adapters solo convierten.
O - Abierto/cerrado:
  · Agregar una regla nueva = nuevo DiscountType + nueva clase DiscountRule + registro en la
    Factory, sin modificar DiscountEngine ni las reglas existentes.
  · El orden lo define order(), no condicionales en el motor.
L - Sustitución de Liskov:
  · Toda DiscountRule cumple el mismo contrato: monto ≥ 0, devuelve 0 si no aplica
    (sin excepciones) y no muta el contexto.
I - Segregación de interfaces:
  · Interfaces de servicio pequeñas y específicas. Separar lectura y escritura donde aporte
    claridad.
  · Prohibido un "EcommerceService" monolítico.
D - Inversión de dependencias:
  · Inyección por constructor (sin @Autowired en campos).
  · Los servicios dependen de interfaces.
  · Clock y DiscountProperties se inyectan; sin LocalDateTime.now() directo ni porcentajes
    hardcodeados.
DRY:
  · Redondeo y operaciones monetarias solo en MoneyUtils (p. ej. percentOf(amount, pct)).
  · /orders reutiliza exactamente el mismo cálculo que /cart/quote.
  · Problem Details se construye solo en GlobalExceptionHandler.
  · Mensajes de error y el mensaje de HU4 como constantes únicas.
  · Validaciones compartidas como anotaciones Bean Validation reutilizables.
Clean Code:
  · Métodos ≤ ~20 líneas, nombres consistentes.
  · Sin números mágicos ni código comentado.
Documenta en docs/arquitectura.md una tabla "Principio → clase(s) → cómo se cumple".

4. PATRONES DE DISEÑO (obligatorios, implementados y probados)
A) STRATEGY — cálculo de cada tipo de descuento
- interface DiscountRule {
    DiscountType type();
    int order();
    DiscountStepResult apply(DiscountContext ctx, BigDecimal currentSubtotal);
  }
- Implementaciones:
  · CategoryDiscountRule (order 1): descuento por línea según el discountPct de su categoría.
  · VolumeDiscountRule (order 2): 5% si el subtotal actual es estrictamente > umbral.
  · CouponDiscountRule (order 3): % del cupón sobre el subtotal actual.
- Contrato común: devuelve 0 si no aplica, nunca un monto negativo, no muta ctx, y redondea
  HALF_UP a 2 decimales vía MoneyUtils.
- interface DiscountCapPolicy { CapResult apply(BigDecimal original, BigDecimal grossDiscount); }
  La implementación PercentageCapPolicy usa un tope desde propiedades.
  Va separada de las reglas: el tope no es un descuento, limita los descuentos.
- DiscountEngine recibe List<DiscountRule> + DiscountCapPolicy. Recorre las reglas en orden,
  encadena el subtotal (cascada) y aplica el tope. No contiene if/switch por tipo de descuento.

B) FACTORY — instanciación de las reglas
- DiscountRuleFactory (@Component) recibe DiscountProperties y expone:
  · List<DiscountRule> createRules(Optional<CouponSnapshot> coupon):
    devuelve las reglas ordenadas; incluye CouponDiscountRule solo si hay un cupón válido.
  · DiscountRule create(DiscountType type, ...):
    switch exhaustivo sobre el enum; un tipo no soportado lanza IllegalArgumentException.
- Es el único punto donde se hace new de reglas.

C) ADAPTER — aislar el motor del modelo JPA
- DiscountContextAdapter: convierte entidades (Product + Category, Coupon) + request en
  DiscountContext (List<DiscountLine>, Optional<CouponSnapshot>).
  DiscountLine y CouponSnapshot son records inmutables sin referencias JPA.
- DiscountResultAdapter: convierte DiscountResult en:
  · CartQuoteResponse (para /cart/quote);
  · Order + OrderItem con snapshots (para /orders).
  Una sola conversión reutilizada.
- Beneficio a documentar: el motor se prueba sin BD ni Spring, y un cambio de esquema solo
  toca el adapter.

D) OBSERVER — eventos del carrito de compras
- Sujeto: CartEventPublisher, que envuelve ApplicationEventPublisher de Spring.
  Los servicios publican; no conocen a los listeners.
- Eventos (records inmutables):
  · CartQuotedEvent (items, discountTotal, capApplied)
  · DiscountCapReachedEvent (subtotalOriginal, grossDiscount, capAdjustment)
  · OrderConfirmedEvent (orderId, orderNumber, total)
- Observadores:
  · CartAuditListener: log estructurado sin datos sensibles.
  · DiscountMetricsListener: contadores Micrometer
      cart.quotes, discount.cap.reached, orders.confirmed.
  · OrderConfirmedEvent se escucha con @TransactionalEventListener(phase = AFTER_COMMIT).
- Regla crítica: los observadores son solo efectos secundarios.
  · NO modifican cálculos, respuesta ni stock.
  · El stock se descuenta dentro de la transacción del checkout, nunca en un listener.
  · Un fallo en un listener no rompe la compra (capturar y registrar).
- El mensaje de HU4 lo arma el mapper a partir de capApplied (determinista), no un listener.
Documenta en docs/arquitectura.md una tabla
"Patrón → problema que resuelve → clases → alternativa descartada y por qué".

5. MAPEO JPA (según 01_schema.sql)
- IDs INT UNSIGNED. DATETIME → LocalDateTime. DECIMAL → BigDecimal. TINYINT(1) → boolean.
- created_at/updated_at los gestiona MySQL: insertable=false, updatable=false o
  @CreationTimestamp.
- Relaciones:
  · Product → Category: @ManyToOne(fetch = LAZY).
  · Order → Coupon: @ManyToOne(fetch = LAZY, optional = true).
  · Order → OrderItem: @OneToMany(mappedBy, cascade = PERSIST) con addItem() que sincroniza
    ambos lados.
  · OrderItem → Product: @ManyToOne(fetch = LAZY).
- Sin recursión en toString/equals/hashCode. Usa @EntityGraph o JOIN FETCH para evitar N+1.
- Snapshots al confirmar la compra:
  · order_items: product_name, category_name, unit_price.
  · orders: coupon_code, discount_cap_pct.

6. REGLAS DE NEGOCIO (cascada multiplicativa, en este orden)
Redondeo: HALF_UP a 2 decimales AL FINAL DE CADA PASO (igual que 02_seed_data.sql).
 R1 Categoría: por línea, line_subtotal × categories.discount_pct, redondeado por línea.
    category_discount_amount de la orden = suma de las líneas.
    line_total = line_subtotal − descuento de la línea.
    subtotal_1 = subtotal_original − categoría.
 R2 Volumen: si subtotal_1 es estrictamente > 100.00, volumen = subtotal_1 × 5%.
    subtotal_2 = subtotal_1 − volumen.
    El porcentaje y el umbral van en propiedades: discount.volume.pct y
    discount.volume.threshold.
 R3 Cupón: válido si el code existe, is_active = 1 y valid_from ≤ ahora ≤ valid_until
    (inclusive). cupón = subtotal_2 × coupons.discount_pct.
 R4 Tope: con discount.cap-pct = 35, bruto = categoría + volumen + cupón y
    tope = subtotal_original × 35%.
    Si bruto > tope:
      cap_adjustment_amount = bruto − tope
      discount_total = tope
      cap_applied = 1
    En otro caso:
      cap_adjustment_amount = 0
      discount_total = bruto
 Cálculo de campos:
  - total = subtotal_original − discount_total.
  - effective_discount_pct = discount_total / subtotal_original × 100.
  - discount_cap_pct = snapshot del tope.
 El servidor recalcula con precios, porcentajes y stock de la BD; ignora valores del cliente.

7. CHECKOUT TRANSACCIONAL (@Transactional)
 1. Validar el request: carrito no vacío, quantity > 0, sin productId duplicados.
 2. Cargar productos + categoría en una sola consulta (404 si falta alguno).
 3. Validar el cupón (si viene).
 4. Calcular reutilizando el mismo flujo de /cart/quote
    (Adapter → Factory → Engine → Adapter).
 5. Descontar stock con un UPDATE atómico por producto:
      UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty
    Si filas afectadas = 0 → InsufficientStockException → rollback total.
 6. order_number con formato ORD-{yyyy}-{6 dígitos}, siguiendo la secuencia del seed
    (ORD-2026-000005, …). Debe ser seguro ante duplicados: UNIQUE + reintento acotado.
 7. Persistir orders + order_items.
 8. Publicar OrderConfirmedEvent (se notifica tras el commit).
 9. Responder con el desglose.

8. ENDPOINTS (/api/v1, respuestas DTO, paginación con Pageable)
 Categorías:
   GET    /categories
   GET    /categories/{id}
   POST   /categories
   PUT    /categories/{id}
   DELETE /categories/{id}   (409 si tiene productos)
 Productos (HU1):
   GET    /products?categoryId=&name=&page=&size=&sort=
   GET    /products/{id}
   POST   /products
   PUT    /products/{id}
   DELETE /products/{id}     (409 si tiene ventas)
   Validar unit_price > 0 y stock ≥ 0.
 Cupones:
   GET    /coupons
   GET    /coupons/{code}
   POST   /coupons
   PUT    /coupons/{id}
   DELETE /coupons/{id}      (409 si fue usado)
   Validar valid_from ≤ valid_until y discount_pct entre 0 y 100.
 Carrito (HU2):
   POST   /cart/quote {items[{productId, quantity}], couponCode?}
   Desglose sin persistir ni tocar stock:
     subtotalOriginal, categoryDiscount, volumeDiscount, couponDiscount, capAdjustment,
     discountTotal, effectiveDiscountPct, capApplied, message, total, currency, items[...].
 Órdenes (HU3):
   POST   /orders            → 201 + Location
   GET    /orders?from=&to=&couponCode=&page=&size=
   GET    /orders/{id}
   GET    /orders/number/{orderNumber}
   Órdenes inmutables (el esquema no tiene status y order_items usa FK RESTRICT).
   La cancelación queda como migración propuesta.
 HU4: si cap_applied = 1, message =
   "¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)".
 Los DTO de entrada no incluyen id, created_at ni montos calculados (anti asignación masiva).
 Documenta en OpenAPI cada endpoint con ejemplos de request y response.

9. ERRORES (Problem Details RFC 9457)
- 400: validación (con la lista de campos).
- 404: recurso inexistente.
- 409: stock insuficiente (productId, solicitada, disponible), violación de FK o UNIQUE.
- 422: cupón inexistente, inactivo o fuera de vigencia.
- 500 genérico sin stack trace ni SQL.

10. PRUEBAS (cobertura ≥ 80%, regla JaCoCo que haga fallar `mvn verify`)
Usa SIEMPRE un Clock fijo (p. ej. 2026-10-15T12:00). WELCOME2026 vence el 2026-12-31 y las
pruebas no deben depender de la fecha real.

Motor de descuentos (sin Spring, @ParameterizedTest).
Casos de referencia del seed, que deben coincidir al centavo:
 (a) Novela×2 + Mouse×1, sin cupón
     → subtotal 50.00 | cat 2.00 | vol 0.00 | cupón 0.00 | dto 2.00 | total 48.00 | 4.00%
 (b) Teclado×1 + Lámpara×1, sin cupón
     → subtotal 110.00 | cat 8.00 | vol 5.10 | dto 13.10 | total 96.90 | 11.91%
 (c) Audífonos×2 + Libro programación×1 + WELCOME2026
     → subtotal 130.00 | cat 9.00 | vol 6.05 | cupón 17.24 | dto 32.29 | total 97.71 | 24.84%
 (d) Monitor×1 + Smartwatch×1 + Balón×2 + TESTCAP30
     → subtotal 320.00 | cat 27.00 | vol 14.65 | cupón 83.51 | ajuste 13.16 | dto 112.00
     | total 208.00 | 35.00% | capApplied = true (HU4)
Bordes:
 - subtotal_1 = 100.00 exacto → sin volumen; 100.01 → con volumen.
 - Bruto exactamente igual al 35% → sin ajuste.
 - Categoría con 0%.
 - Cascada ≠ suma de porcentajes.
 - Cuadre: total + discount_total = subtotal_original.

Patrones:
 - Strategy:
   · cada regla por separado (aplica / no aplica / bordes);
   · DiscountEngine con reglas mock verifica el orden y la cascada;
   · las reglas no mutan el contexto.
 - Factory:
   · sin cupón → 2 reglas; con cupón → 3 reglas, en orden 1-2-3;
   · un tipo no soportado lanza excepción;
   · los porcentajes se toman de DiscountProperties.
 - Adapter:
   · entidades → DiscountContext exacto;
   · DiscountResult → Order/OrderItem con los snapshots y montos del caso (d).
 - Observer (@RecordApplicationEvents):
   · quote publica CartQuotedEvent;
   · el caso (d) publica DiscountCapReachedEvent;
   · un checkout exitoso publica OrderConfirmedEvent;
   · un checkout fallido NO lo notifica tras el rollback;
   · un listener que falla no afecta la respuesta HTTP.
 - OCP: una regla falsa registrada en la Factory se aplica sin modificar el motor.

Servicios (Mockito):
 - Producto 5 (stock 0) → 409.
 - Producto 7 con quantity 3 → 409; con quantity 2 → OK.
 - Producto inexistente → 404.
 - Carrito vacío, quantity ≤ 0 o IDs duplicados → 400.
 - Cupones:
   · EXPIRED2025 → 422
   · INACTIVE10 → 422
   · código inexistente → 422
   · WELCOME2026 en los límites exactos de valid_from/valid_until → válido
 - Stock insuficiente → no se persiste la orden.
 - order_number siguiente = ORD-2026-000005.
 - Snapshots copiados correctamente.

Repositorios (@DataJpaTest + Testcontainers mysql:5.7.44):
 - init con 01_schema.sql + 02_seed_data.sql;
 - validate pasa;
 - lectura de las 4 órdenes del seed con sus ítems sin N+1;
 - UPDATE atómico de stock;
 - restricciones UNIQUE y FK.

Integración (@SpringBootTest + MockMvc + Testcontainers):
 - todos los endpoints con casos felices y de error;
 - reproducir vía POST /orders los casos (c) y (d) y verificar en BD la orden, sus ítems
   y el stock descontado;
 - rollback ante fallo.

Los scripts hacen DROP TABLE: solo pueden ejecutarse en el contenedor de pruebas, nunca en dev.

11. ENTREGABLES
- pom.xml, application.properties, application-dev.properties, application-test.properties.
- Código completo y colección Postman o archivo .http.
- OpenAPI en /swagger-ui.html.
- README.md: requisitos, creación de la BD con 01 y 02, variables de entorno,
  `mvn clean verify`, `mvn spring-boot:run`, ubicación del reporte JaCoCo.
- docs/arquitectura.md: por qué MVC por capas, responsabilidad de cada capa, modelo de datos,
  reglas con los ejemplos del seed, tabla SOLID/DRY, tabla de patrones, decisiones
  (UPDATE atómico, órdenes inmutables, snapshots, redondeo por paso, Observer sin efectos en
  el cálculo), trade-offs y seguridad pendiente.
- Aportes para docs/ia.md: prompts usados y errores típicos de IA corregidos:
  · sumar porcentajes en lugar de cascada;
  · usar double;
  · redondear solo al final;
  · aplicar el tope sobre el total con descuento;
  · ddl-auto=update;
  · N+1;
  · lógica duplicada entre quote y checkout;
  · descontar stock dentro de un listener.

12. FORMA DE TRABAJO
1) Antes de codificar, presenta: supuestos, mapeo tabla → entidad, endpoints, diseño de los
   patrones, plan de pruebas, riesgos y decisiones pendientes.
2) Entrega por incrementos:
   a. pom + configuración;
   b. entidades + repositorios + tests de repositorio;
   c. motor de descuentos (Strategy + Factory + Adapter) + tests con los casos a–d;
   d. servicios + tests;
   e. controllers + DTO + errores + Observer + pruebas de integración;
   f. documentación.
   En cada incremento indica las rutas completas. Prohibido "resto del código" o pseudocódigo.
3) No afirmes que compila ni que alcanza el 80% sin haberlo ejecutado. Si no puedes ejecutarlo,
   dilo y da los comandos de verificación.
4) Sugiere commits descriptivos por incremento.
```

</details>

<details>
<summary><strong>P3 · Frontend senior Angular</strong></summary>

```text
Actúa como desarrollador Frontend Senior experto en Angular (versión estable actual, standalone components, signals y TypeScript estricto).

## Objetivo
Crear la interfaz gráfica de un carrito de compras en la carpeta `apps/frontend`, consumiendo un backend Spring Boot ya existente. La UI debe ser SENCILLA y funcional (sin librerías de UI; CSS mínimo). Los estilos se trabajarán después.

## Backend existente (NO modificar, respetar contratos)
Base URL: http://localhost:8080/api/v1 (configurable en environment.ts)

| Método | Endpoint      | Body        | Respuesta                 |
|--------|---------------|-------------|---------------------------|
| GET    | /products     | —           | ProductResponse[]         |
| POST   | /cart/quote   | CartRequest | CartQuoteResponse         |
| POST   | /orders       | CartRequest | OrderResponse (201)       |

## Interfaces TypeScript (usar EXACTAMENTE estos nombres de campos)
// BigDecimal llega como number; LocalDateTime como string ISO.

export interface ProductResponse {
  id: number;
  name: string;
  unitPrice: number;
  stock: number;
  categoryId: number;
  categoryName: string;
}

export interface CartItemRequest {
  productId: number;   // > 0
  quantity: number;    // > 0
}

export interface CartRequest {
  items: CartItemRequest[];   // no vacío, productId únicos (sin repetidos)
  couponCode: string | null;  // máx. 50 caracteres; null si no hay cupón
}

export interface CartLineResponse {
  productId: number;
  productName: string;
  categoryName: string;
  unitPrice: number;
  quantity: number;
  lineSubtotal: number;
  categoryDiscount: number;
  lineTotal: number;
}

export interface CartQuoteResponse {
  subtotalOriginal: number;
  categoryDiscount: number;
  volumeDiscount: number;
  couponDiscount: number;
  capAdjustment: number;
  discountTotal: number;
  effectiveDiscountPct: number;
  discountCapPct: number;
  capApplied: boolean;
  message: string | null;
  total: number;
  currency: string;
  couponCode: string | null;
  items: CartLineResponse[];
}

export interface OrderItemResponse extends CartLineResponse {
  id: number;
}

export interface OrderResponse {
  id: number;
  orderNumber: string;
  createdAt: string;
  couponCode: string | null;
  subtotalOriginal: number;
  categoryDiscount: number;
  volumeDiscount: number;
  couponDiscount: number;
  capAdjustment: number;
  discountTotal: number;
  effectiveDiscountPct: number;
  discountCapPct: number;
  capApplied: boolean;
  message: string | null;
  total: number;
  currency: string;
  items: OrderItemResponse[];
}

## Historias de usuario
- HU1: Listar productos (id, nombre, precio, categoría, stock). Agregar/quitar productos y
  modificar cantidades en tiempo real; el subtotal original (Σ unitPrice × quantity) se
  actualiza al instante en el cliente. No permitir superar el stock. Si se agrega un producto
  ya existente, incrementar su cantidad (el backend rechaza productId repetidos).
- HU2: Input de cupón + botón "Aplicar" → POST /cart/quote. Mostrar: Descuento de Categoría
  (categoryDiscount), Descuento por Volumen (volumeDiscount), Descuento por Cupón
  (couponDiscount), Ajuste por tope (capAdjustment, solo si > 0), % efectivo
  (effectiveDiscountPct), ahorro total (discountTotal) y total a pagar (total). Mostrar el
  detalle por línea (items). Mostrar errores del backend (cupón inválido, vencido, etc.).
  Si el carrito cambia, invalidar el desglose o recotizar automáticamente.
- HU3: Botón "Finalizar compra" → POST /orders. Mostrar confirmación con orderNumber,
  createdAt y totales desglosados. Luego vaciar el carrito y recargar productos para
  reflejar el nuevo stock. Manejar error de stock insuficiente.
- HU4: Si capApplied = true (en quote u orden), mostrar alerta visual persistente y
  distintiva con el texto exacto:
  "¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)".
  Usar el campo message si el backend lo envía; si no, usar el texto fijo.

## Reglas técnicas
- El frontend NO calcula descuentos; solo el subtotal original. Todo lo demás viene del backend.
- Estructura: models/, services/ (ProductService, CartService con signals, OrderService),
  components/ (product-list, cart, coupon-form, discount-summary, cap-alert,
  order-confirmation).
- HttpClient con manejo de errores (mostrar mensaje del body de error) y estados de carga.
- Monedas con CurrencyPipe usando el campo currency; porcentajes con DecimalPipe.
- proxy.conf.json para /api → http://localhost:8080 (evitar CORS en desarrollo).
- Pruebas unitarias básicas de CartService.

## Entregables
1. Comandos para crear el proyecto en apps/frontend.
2. Código completo de cada archivo.
3. Instrucciones para ejecutarlo.
```

</details>

<details>
<summary><strong>P4 · Seguridad JWT y autorización en Swagger</strong></summary>

```text
Actúa como desarrollador senior Spring Boot 3 / Java 17+. Agrega seguridad JWT y
autorización en Swagger al backend existente del KATA E-Commerce.

RESTRICCIONES (OBLIGATORIAS)
- NO modifiques la lógica de negocio: PricingService, OrderService, ProductService,
  motor de descuentos (categoría → volumen → cupón, tope 35 %), DTOs, entidades,
  repositorios, scripts SQL ni el GlobalExceptionHandler existente (solo puedes
  reutilizar su formato de error).
- En CartController, OrderController y ProductController solo puedes AGREGAR
  la anotación @SecurityRequirement(name = "bearerAuth"). No cambies rutas,
  firmas, códigos HTTP ni respuestas.
- Todas las pruebas unitarias y de integración existentes deben seguir pasando.
  Si algún test de controlador falla por seguridad, ajústalo solo con
  @WithMockUser o con un token, nunca cambiando sus aserciones.

IMPLEMENTAR
1. Dependencias: spring-boot-starter-security y jjwt (api, impl, jackson 0.12.x).
2. Paquete com.examen.ecommerce.security:
   - JwtService: generar y validar tokens HS256. Lee app.jwt.secret (Base64,
     mínimo 256 bits) y app.jwt.expiration-ms (3600000) desde application.yml.
   - JwtAuthenticationFilter (OncePerRequestFilter): lee "Authorization: Bearer".
   - SecurityConfig:
     · stateless, CSRF deshabilitado.
     · Mantener la configuración CORS actual del frontend.
     · Públicos: POST /api/v1/auth/login, /v3/api-docs/**, /swagger-ui/**,
       /swagger-ui.html.
     · Autenticados: /api/v1/products/**, /api/v1/cart/**, /api/v1/orders/**.
   - AuthenticationEntryPoint que responda 401 JSON (no 403) si falta el token o
     es inválido o expirado, con el mismo formato de error del proyecto.
   - Usuario en memoria con BCrypt: app.security.user=admin,
     app.security.password=admin123 (configurables).
3. AuthController: POST /api/v1/auth/login con body {username, password}.
   - Responde 200 {accessToken, tokenType:"Bearer", expiresIn}.
   - Responde 401 si las credenciales son inválidas.
   - Tag Swagger "Autenticación".
4. OpenAPI: bean con SecurityScheme "bearerAuth" (HTTP, bearer, JWT) para que
   aparezca el botón "Authorize" en Swagger UI.
5. Frontend: solo agrega un interceptor HTTP que haga login y envíe el header
   Bearer. No toques la lógica de carrito, desglose ni la alerta del 35 %.
6. Tests nuevos:
   - 401 sin token.
   - 401 con token inválido.
   - 200 en el login.
   - 200 en /products con token.

ENTREGABLE
Lista de archivos creados y modificados, más los pasos para probar en Swagger:
login → Authorize → ejecutar.
```

</details>
