# Gobernanza y uso de IA

## Prompts usados

1. **Prompt de base de datos** (`PROMPT BASE DE DATOS MYSQL.txt`): generó `00_create_database.sql`, `01_schema.sql`, `02_seed_data.sql`, `03_validation_queries.sql` y `base-datos.md` (5 tablas, DECIMAL UNSIGNED, FK RESTRICT, snapshots, órdenes de referencia con sus cálculos).
2. **Prompt de desarrollador senior** (`PROMPT DESARROLLADOR SENIOR.txt`): rol de backend senior Java, fuentes de verdad (01 y 02), stack cerrado (Boot 3.2.x, MySQL 5.7.44, sin Flyway), arquitectura por capas, SOLID/DRY verificables, 4 patrones obligatorios, reglas en cascada, checkout transaccional, Problem Details, plan de pruebas con los casos a–d y entrega por incrementos.
3. Prompts de seguimiento recomendados: "verifica al centavo los casos a–d con BigDecimal", "explica por qué falla `validate` con UNSIGNED", "demuestra que el listener AFTER_COMMIT no se ejecuta con rollback" (este último ya no aplica: el Observer se eliminó, ver más abajo).
4. Iteraciones posteriores según el historial de git: reducción del código al alcance de las HU con Claude Code (`9a8c8a3`), frontend Angular generado con Claude Code (`4ff0aac`), autenticación JWT en backend y frontend (`2e41d2d`, `9c5feb5`) y usuarios en la tabla `app_users` (`7d77168`, `d4b433d`). Pendiente: los prompts de estas iteraciones no están en el repositorio.

## Corrección de alcance: sobreingeniería del código generado

El primer código generado por el prompt de desarrollador senior (`b5259c1`) incluía mucho más de lo que pedían las historias de usuario: CRUD de categorías, productos y cupones, consulta de órdenes, paginación, paquetes `adapter/` y `event/` (Observer con métricas), `service/impl/` con interfaces, `Clock` inyectado y validadores adicionales. En `9a8c8a3` se limitó el código a las HU1–HU4 y a los casos de negocio.

Consecuencia: con esa reducción también se eliminaron los patrones Strategy, Factory, Adapter y Observer, que cubrían el requisito de "al menos dos patrones de diseño", y la documentación no se actualizó en ese momento. Corrección posterior con Claude Code: se reintrodujeron solo **Strategy** (una clase por regla de descuento) y **Factory** (`DiscountRuleFactory`), sin sobreingeniería (sin beans extra, sin política de tope separada), manteniendo la API de `DiscountCalculator` y pasando todas las pruebas existentes sin modificarlas (ver `arquitectura.md` §7).

## Errores típicos de IA detectados y corregidos

| Error típico | Por qué está mal | Corrección en este proyecto |
|---|---|---|
| **Sumar porcentajes** (5 % + 15 % = 20 % sobre el mismo subtotal) | El negocio es cascada multiplicativa: cada paso se aplica sobre el subtotal anterior. En el caso (c), sumar daría 24.20 en vez de 23.29 | `DiscountCalculator` pasa a cada `DiscountRule` el subtotal que dejó la anterior; prueba `CouponDiscountTest` ("Se calcula sobre el subtotal tras la regla 2, no sobre el original") |
| **Usar `double`** | Errores binarios de punto flotante (0.1 + 0.2 ≠ 0.3) | Solo `BigDecimal`; columnas DECIMAL; `MoneyUtils` |
| **Redondear solo al final** | El seed redondea en cada paso: 83.505 → 83.51 antes de sumar; redondear al final cambia centavos | `percentOf` redondea HALF_UP a 2 decimales en cada regla y cada línea |
| **Aplicar el tope sobre el total con descuento** | El tope es el 35 % del `subtotal_original` (320 × 35 % = 112.00), no del total ya descontado | `DiscountCalculator`: `cap = percentOf(original, capPct)`; prueba `DiscountCapTest` |
| **`ddl-auto=update`** | Altera un esquema que es fuente de verdad y oculta incompatibilidades | `validate` + `columnDefinition`/`@JdbcTypeCode` (arquitectura §8) |
| **N+1** | Cargar productos/ítems uno a uno multiplica consultas | `JOIN FETCH` (`findAllWithCategoryByIdIn`) y `@EntityGraph` (`findAllByOrderByIdAsc`). Pendiente: ninguna prueba cuenta las sentencias (la prueba con Hibernate Statistics se eliminó) |
| **Lógica duplicada entre quote y checkout** | Dos cálculos divergen con el tiempo | `PricingService.price` único, reutilizado por `PricingService.quote` y `OrderService` |
| **Descontar stock dentro de un listener** | El listener corre fuera de la transacción (o tras el commit): se pierde la atomicidad y un fallo no hace rollback | `StockService.decrement` con `Propagation.MANDATORY` dentro de la transacción del checkout (hoy no hay listeners) |
| Usar `LocalDateTime.now()` directo | Pruebas dependientes de la fecha real (WELCOME2026 vence el 2026-12-31) | Se eliminó en `9a8c8a3` y se **reintrodujo** después: `TimeConfig` publica un bean `Clock` que reciben `CouponValidator` y `OrderService` (`LocalDateTime.now(clock)`); `CouponValidatorTest` fija la fecha con `Clock.fixed` y prueba el último segundo de vigencia y el día siguiente |
| Subir a Spring Boot 3.4/3.5 "por estar más nuevo" | Hibernate 6.6+ no soporta MySQL 5.7 | Boot 3.2.12 justificado en el `pom.xml` |
| Usar Testcontainers por defecto de Boot 3.2 (1.19.x) | No funciona con Docker Engine 29 | **No vigente:** las dependencias siguen en el `pom.xml`, pero ninguna prueba usa Testcontainers; las pruebas con BD corren contra el MySQL local |
| Afirmar que "compila y cubre 80 %" sin ejecutarlo | No verificable | Pendiente: el `pom.xml` no incluye JaCoCo, así que la cobertura no se mide (ver README §5) |
| Documentación que describe código eliminado | Tras la reducción de alcance, README y `arquitectura.md` seguían citando endpoints, clases y patrones inexistentes | Revisión de la documentación contra el código (esta actualización) |
