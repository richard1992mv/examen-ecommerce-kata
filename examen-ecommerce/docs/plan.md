# Plan previo a la codificación

> **Estado tras la implementación.** Este documento se conserva como registro del plan original y no describe el código actual. Tras la reducción de alcance (commit `9a8c8a3`) no existen los patrones Strategy, Factory, Adapter ni Observer, ni las clases del plan de pruebas de abajo. Las pruebas reales son 12 clases: unitarias del cálculo y del cupón, `@WebMvcTest` de seguridad y `@SpringBootTest` contra el MySQL local, sin Testcontainers. Además se añadió autenticación JWT con la tabla `app_users`, que no estaba en el plan. Para el estado vigente, ver `arquitectura.md` y la sección 5 del `README.md`.

## Supuestos

1. El esquema de `01_schema.sql` es la fuente de verdad: no se crean tablas ni columnas.
2. El carrito vive en el frontend; el backend recibe `items[{productId, quantity}]` y `couponCode` opcional, e ignora cualquier precio o porcentaje enviado.
3. Moneda única USD (`orders.currency DEFAULT 'USD'`).
4. Zona horaria de negocio `America/Bogota`; las fechas `DATETIME` del seed y la vigencia de los cupones se interpretan en esa zona.
5. La vigencia del cupón es inclusiva en ambos extremos y se compara con precisión de segundos (precisión de `DATETIME`).
6. `/cart/quote` también verifica stock (409) para avisar antes del checkout; el checkout lo vuelve a garantizar con el UPDATE atómico.
7. Un código de cupón vacío o ausente significa "sin cupón"; uno inexistente es 422.

## Mapeo tabla → entidad

Ver `arquitectura.md` §3.

## Endpoints

Ver `README.md` §6 y Swagger (`/swagger-ui.html`).

## Diseño de patrones

Strategy (`DiscountRule`, `DiscountCapPolicy`), Factory (`DiscountRuleFactory`), Adapter (`DiscountContextAdapter`, `DiscountResultAdapter`), Observer (`CartEventPublisher` + listeners). Detalle en `arquitectura.md` §7.

## Plan de pruebas

| Nivel | Clases | Infraestructura |
|---|---|---|
| Motor (casos a–d, bordes, Strategy, Factory, OCP) | `DiscountEngineSeedCasesTest`, `DiscountEngineTest`, `DiscountRulesTest`, `PercentageCapPolicyTest`, `DiscountRuleFactoryTest`, `OpenClosedPrincipleTest`, `MoneyUtilsTest` | Ninguna (JUnit + Mockito) |
| Adapter | `DiscountAdaptersTest` | Ninguna |
| Servicios | `CartValidatorImplTest`, `CouponValidatorImplTest`, `StockServiceImplTest`, `PricingServiceImplTest`, `CartServiceImplTest`, `TransactionalOrderCheckoutTest`, `OrderServiceImplTest`, `CatalogServicesTest`, `OrderQueryServiceImplTest` | Mockito, Clock fijo |
| Observer | `ObserverUnitTest` + ITs con `@RecordApplicationEvents` | SimpleMeterRegistry / contexto completo |
| Web | `ApiErrorHandlingWebTest` | `@WebMvcTest` |
| Repositorios | `RepositoryIT` | `@DataJpaTest` + Testcontainers mysql:5.7.44 |
| Integración | `CatalogApiIT`, `CartApiIT`, `OrderApiIT` | `@SpringBootTest` + MockMvc + Testcontainers |
| Arquitectura | `DiscountPackagePurityTest` | Lectura de fuentes |

## Riesgos

| Riesgo | Mitigación |
|---|---|
| `validate` falla por columnas UNSIGNED/TINYINT/CHAR | `columnDefinition`, `@JdbcTypeCode`, `tinyInt1isBit=false` (§8 de arquitectura) |
| Hibernate ≥ 6.6 no soporta MySQL 5.7 | Boot fijado en 3.2.12 (Hibernate 6.4) |
| Testcontainers antiguo incompatible con Docker 29 | `testcontainers.version=1.21.4` |
| Imagen `mysql:5.7.44` sin variante arm64 | En Mac Apple Silicon usar emulación (`DOCKER_DEFAULT_PLATFORM=linux/amd64`) |
| Colisión de `order_number` concurrente | UNIQUE + reintento acotado |
| Desfase horario en `DATETIME` | Misma zona en Clock, JDBC, Hibernate y JVM |

## Decisiones pendientes (para el negocio)

1. Cancelación/devolución de órdenes (requiere `status` → `migraciones-propuestas.md`).
2. ¿El cupón debe tener límite de usos por cliente o global? (requiere columnas nuevas).
3. ¿Se reserva stock al cotizar? (hoy no).
4. Moneda múltiple (hoy USD fijo).
