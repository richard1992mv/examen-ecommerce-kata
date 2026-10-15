# examen-ecommerce · Backend

KATA **Core E-Commerce con Sistema de Descuentos Acumulativos**.
API REST en Java 17 + Spring Boot 3.2.12 sobre MySQL 5.7.44 (base `kata_ecommerce`).

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 (compila con `--release 17`; también corre en 21) |
| Maven | 3.9+ |
| MySQL | 5.7.44 (dev) |
| Docker | Docker Desktop / Engine con soporte de Testcontainers (solo para `mvn verify`) |

> Testcontainers está fijado en **1.21.4**: las versiones anteriores no funcionan con Docker Engine 29 (API mínima 1.44).

## 2. Estructura del monorepo

```
examen-ecommerce/
├─ database/                 ← fuente única de los scripts SQL
│   ├─ 00_create_database.sql
│   ├─ 01_schema.sql         (DROP TABLE + CREATE: solo en una BD nueva o en el contenedor de pruebas)
│   ├─ 02_seed_data.sql
│   └─ 03_validation_queries.sql
└─ apps/backend/             ← este proyecto
    ├─ src/main/java/com/examen/ecommerce/...
    ├─ src/test/java/...
    ├─ docs/ (arquitectura.md, ia.md, plan.md, migraciones-propuestas.md)
    └─ http/ecommerce.http
```

Las pruebas copian `01_schema.sql` y `02_seed_data.sql` a `target/test-classes/db/` (ver `<testResources>` del `pom.xml`). **Nunca** se empaquetan en el JAR ni se ejecutan contra dev (`spring.sql.init.mode=never`).

## 3. Crear la base de datos (dev)

```bash
mysql -u root -p < ../../database/00_create_database.sql
mysql -u root -p kata_ecommerce < ../../database/01_schema.sql
mysql -u root -p kata_ecommerce < ../../database/02_seed_data.sql
# opcional: comprobar los cálculos del seed
mysql -u root -p kata_ecommerce < ../../database/03_validation_queries.sql
```

⚠️ `01_schema.sql` hace `DROP TABLE`: ejecútalo solo para crear la BD por primera vez o para reiniciarla a propósito.

## 4. Variables de entorno

| Variable | Ejemplo | Obligatoria |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/kata_ecommerce?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&tinyInt1isBit=false&connectionTimeZone=America/Bogota` | No (ese es el valor por defecto) |
| `DB_USER` | `kata` | Sí |
| `DB_PASSWORD` | `****` | Sí |
| `SERVER_PORT` | `8080` | No |

`tinyInt1isBit=false` es necesario para que `ddl-auto=validate` acepte las columnas `TINYINT(1) UNSIGNED` (ver `docs/arquitectura.md` §8).

PowerShell:

```powershell
$env:DB_USER="kata"; $env:DB_PASSWORD="kata"
```

Bash:

```bash
export DB_USER=kata DB_PASSWORD=kata
```

## 5. Compilar, probar y ejecutar

```bash
# Compila, ejecuta TODAS las pruebas (unitarias + Testcontainers) y aplica la regla JaCoCo ≥ 80 %
mvn clean verify

# Arranca la API con el perfil dev
mvn spring-boot:run
```

- Reporte JaCoCo: `target/site/jacoco/index.html` (la regla de `jacoco:check` hace fallar `mvn verify` por debajo del 80 % de líneas e instrucciones).
- Swagger UI: <http://localhost:8080/swagger-ui.html> · OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Actuator: `/actuator/health`, `/actuator/metrics` (contadores `cart.quotes`, `discount.cap.reached`, `orders.confirmed`).
- Peticiones de ejemplo: `http/ecommerce.http` (IntelliJ HTTP Client / VS Code REST Client).

Las pruebas usan un reloj fijo (`2026-10-15T12:00-05:00`, `application-test.properties`), así que no dependen de la fecha real (WELCOME2026 vence el 2026-12-31).

## 6. Endpoints (`/api/v1`)

| Recurso | Método y ruta | Notas |
|---|---|---|
| Categorías | `GET /categories`, `GET /categories/{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` | 409 si tiene productos |
| Productos (HU1) | `GET /products?categoryId=&name=&page=&size=&sort=`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` | `unitPrice > 0`, `stock ≥ 0`; 409 si tiene ventas |
| Cupones | `GET /coupons`, `GET /coupons/{code}`, `POST`, `PUT /{id}`, `DELETE /{id}` | `validFrom ≤ validUntil`, `0 ≤ discountPct ≤ 100`; 409 si fue usado |
| Carrito (HU2/HU4) | `POST /cart/quote` | Desglose sin persistir ni tocar stock |
| Órdenes (HU3) | `POST /orders` (201 + Location), `GET /orders?from=&to=&couponCode=`, `GET /orders/{id}`, `GET /orders/number/{orderNumber}` | Órdenes inmutables |

Errores en formato Problem Details (RFC 9457): 400 validación (con `errors[]`), 404, 409 (stock con `productId/requested/available`, FK, UNIQUE), 422 cupón, 500 genérico.

## 7. Seguridad (pendiente)

Fuera de alcance en esta fase: no hay Spring Security ni JWT. Solo CORS configurable por perfil (`app.cors.*`). Ver `docs/arquitectura.md` §11.

## 8. Commits sugeridos por incremento

```
chore(backend): pom, perfiles y configuración base (Boot 3.2.12, MySQL 5.7, sin Flyway)
feat(persistence): entidades JPA, repositorios y pruebas @DataJpaTest con Testcontainers
feat(discounts): motor Strategy + Factory + Adapter con casos a–d del seed
feat(services): catálogo, cupones, carrito, stock y checkout transaccional
feat(api): controllers, DTO, Problem Details, Observer y pruebas de integración
docs: README, arquitectura, IA y colección HTTP
```
