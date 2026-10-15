# Aportes para docs/ia.md

## Prompts usados

1. **Prompt de base de datos** (`PROMPT BASE DE DATOS MYSQL.txt`): generó `00_create_database.sql`, `01_schema.sql`, `02_seed_data.sql`, `03_validation_queries.sql` y `base-datos.md` (5 tablas, DECIMAL UNSIGNED, FK RESTRICT, snapshots, órdenes de referencia con sus cálculos).
2. **Prompt de desarrollador senior** (`PROMPT DESARROLLADOR SENIOR.txt`): rol de backend senior Java, fuentes de verdad (01 y 02), stack cerrado (Boot 3.2.x, MySQL 5.7.44, sin Flyway), arquitectura por capas, SOLID/DRY verificables, 4 patrones obligatorios, reglas en cascada, checkout transaccional, Problem Details, plan de pruebas con los casos a–d y entrega por incrementos.
3. Prompts de seguimiento recomendados: "verifica al centavo los casos a–d con BigDecimal", "explica por qué falla `validate` con UNSIGNED", "demuestra que el listener AFTER_COMMIT no se ejecuta con rollback".

## Errores típicos de IA detectados y corregidos

| Error típico | Por qué está mal | Corrección en este proyecto |
|---|---|---|
| **Sumar porcentajes** (5 % + 15 % = 20 % sobre el mismo subtotal) | El negocio es cascada multiplicativa: cada paso se aplica sobre el subtotal anterior. En el caso (c), sumar daría 24.20 en vez de 23.29 | `DiscountEngine` encadena `currentSubtotal`; prueba `cascadeIsNotTheSumOfPercentages` |
| **Usar `double`** | Errores binarios de punto flotante (0.1 + 0.2 ≠ 0.3) | Solo `BigDecimal`; columnas DECIMAL; `MoneyUtils` |
| **Redondear solo al final** | El seed redondea en cada paso: 83.505 → 83.51 antes de sumar; redondear al final cambia centavos | `percentOf` redondea HALF_UP a 2 decimales en cada regla y cada línea |
| **Aplicar el tope sobre el total con descuento** | El tope es el 35 % del `subtotal_original` (320 × 35 % = 112.00), no del total ya descontado | `PercentageCapPolicy.apply(original, gross)` |
| **`ddl-auto=update`** | Altera un esquema que es fuente de verdad y oculta incompatibilidades | `validate` + `columnDefinition`/`@JdbcTypeCode` (arquitectura §8) |
| **N+1** | Cargar productos/ítems uno a uno multiplica consultas | `JOIN FETCH` y `@EntityGraph`; `RepositoryIT` exige 1 sentencia con Hibernate Statistics |
| **Lógica duplicada entre quote y checkout** | Dos cálculos divergen con el tiempo | `PricingService` único reutilizado por `CartServiceImpl` y `TransactionalOrderCheckout` |
| **Descontar stock dentro de un listener** | El listener corre fuera de la transacción (o tras el commit): se pierde la atomicidad y un fallo no hace rollback | `StockService.decrement` con `Propagation.MANDATORY` dentro del checkout; los listeners solo registran |
| Usar `LocalDateTime.now()` directo | Pruebas dependientes de la fecha real (WELCOME2026 vence el 2026-12-31) | `Clock` inyectado y fijo en pruebas |
| Subir a Spring Boot 3.4/3.5 "por estar más nuevo" | Hibernate 6.6+ no soporta MySQL 5.7 | Boot 3.2.12 justificado en el `pom.xml` |
| Usar Testcontainers por defecto de Boot 3.2 (1.19.x) | No funciona con Docker Engine 29 | `testcontainers.version=1.21.4` |
| Afirmar que "compila y cubre 80 %" sin ejecutarlo | No verificable | Ver nota de verificación en el README / entrega |
