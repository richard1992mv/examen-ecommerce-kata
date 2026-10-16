# examen-ecommerce · Backend

KATA **Core E-Commerce con Sistema de Descuentos Acumulativos**.
API REST en Java 17 + Spring Boot 3.2.12 sobre MySQL 5.7.44 (base `kata_ecommerce`).

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 (compila con `--release 17`; también corre en 21) |
| Maven | 3.9+ |
| MySQL | 5.7.44 (dev y pruebas con BD) |

> El `pom.xml` declara Testcontainers (`junit-jupiter`, `mysql`), pero hoy ninguna prueba lo usa: las pruebas con BD corren contra el MySQL local (ver §5).

## 2. Estructura del monorepo

```
examen-ecommerce/
├─ apps/backend/             ← este proyecto (API REST)
│   ├─ src/main/java/com/examen/ecommerce/...
│   └─ src/test/java/...
├─ apps/frontend/            ← Angular 22 (ver apps/frontend/README.md)
├─ docs/
│   ├─ arquitectura.md, ia.md, plan.md, migraciones-propuestas.md
│   └─ db_kata_ecommerce/    ← fuente única de los scripts SQL + diccionario y diagrama ER
│       ├─ 00_create_database.sql
│       ├─ 01_schema.sql     (DROP TABLE + CREATE: solo en una BD nueva)
│       ├─ 02_seed_data.sql
│       ├─ 03_validation_queries.sql
│       ├─ 04_auth_users.sql (solo para una BD ya existente: crea app_users)
│       └─ base-datos.md
└─ postman/                  ← colección de pruebas de integración HU1–HU4 con JWT
```

Los scripts SQL **no** se empaquetan en el JAR ni se ejecutan desde la aplicación (`spring.sql.init.mode=never`, `ddl-auto=validate`).

## 3. Crear la base de datos (dev)

Desde `examen-ecommerce/`:

```bash
mysql -u root -p < docs/db_kata_ecommerce/00_create_database.sql
mysql -u root -p kata_ecommerce < docs/db_kata_ecommerce/01_schema.sql
mysql -u root -p kata_ecommerce < docs/db_kata_ecommerce/02_seed_data.sql
# opcional: comprobar los cálculos del seed
mysql -u root -p kata_ecommerce < docs/db_kata_ecommerce/03_validation_queries.sql
```

⚠️ `01_schema.sql` hace `DROP TABLE`: ejecútalo solo para crear la BD por primera vez o para reiniciarla a propósito.

Si la base ya existía antes de la autenticación JWT, no hace falta reiniciarla: basta con `04_auth_users.sql` (re-ejecutable, solo crea y carga `app_users`).

## 4. Configuración

Toda la configuración está en `src/main/resources/application.properties` (común) y `application-dev.properties` (perfil por defecto).

| Propiedad | Variable de entorno | Uso |
|---|---|---|
| `server.port` | `SERVER_PORT` (por defecto `8080`) | Puerto HTTP |
| `spring.datasource.url`, `username`, `password` | — | Conexión a `kata_ecommerce`. **Hoy tienen valores fijos en el archivo**; ajústalos a tu MySQL local |
| `app.jwt.secret` | `JWT_SECRET` | Secreto HS256 en Base64 (mínimo 256 bits). El valor por defecto es solo para dev |
| `app.jwt.expiration-ms` | `JWT_EXPIRATION_MS` (por defecto 1 h) | Vigencia del token |
| `app.cors.allowed-origins` | — | Orígenes CORS (en `application-dev.properties`) |
| `discount.volume.pct`, `discount.volume.threshold`, `discount.cap-pct` | — | Reglas de volumen y tope (5 %, 100.00, 35 %) |
| `order.number.prefix`, `order.number.max-attempts` | — | Formato `ORD-yyyy-NNNNNN` y reintentos por colisión |

> Pendiente: leer las credenciales de la BD desde variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`) en lugar de fijarlas en `application.properties`.

## 5. Compilar, probar y ejecutar

```bash
# Compila y ejecuta todas las pruebas (requiere MySQL local con el seed cargado)
mvn clean verify

# Una sola clase o un solo método
mvn test -Dtest=DiscountCapTest
mvn test -Dtest=CheckoutOrderTest#nombreDelMetodo

# Arranca la API con el perfil dev
mvn spring-boot:run
```

- Swagger UI: <http://localhost:8080/swagger-ui.html> · OpenAPI JSON: <http://localhost:8080/v3/api-docs> (botón "Authorize" para pegar el token).
- Peticiones de ejemplo: colección Postman en `postman/`.

Tipos de prueba:

| Tipo | Clases | Infraestructura |
|---|---|---|
| Unitarias | `CategoryDiscountTest`, `VolumeDiscountTest`, `CouponDiscountTest`, `DiscountCapTest`, `DiscountRuleFactoryTest`, `CouponValidatorTest`, `DatabaseUserDetailsServiceTest` | Ninguna |
| Web + seguridad | `SecurityIntegrationTest` | `@WebMvcTest` con repositorios simulados |
| Con BD | `ProductCatalogTest`, `CouponBreakdownTest`, `CheckoutOrderTest`, `DiscountCapAlertTest`, `AuthLoginDatabaseTest` | `@SpringBootTest` + perfil `dev` contra el MySQL local; `@Transactional` deshace los cambios al terminar |

La hora se inyecta como `Clock` (`config/TimeConfig`). Las pruebas unitarias de `CouponValidatorTest` usan un reloj fijo, pero las pruebas con BD usan el reloj real del bean: dependen de la fecha actual porque `WELCOME2026` vence el 2026-12-31.

> Pendiente: el requisito de cobertura mínima del 80 % **no se mide** hoy (el `pom.xml` no incluye JaCoCo).

## 6. Endpoints (`/api/v1`)

| Recurso | Método y ruta | Notas |
|---|---|---|
| Autenticación | `POST /auth/login` | Público. Devuelve `accessToken`, `tokenType` y `expiresIn` (segundos) |
| Productos (HU1) | `GET /products` | Catálogo completo ordenado por id, con categoría |
| Carrito (HU2/HU4) | `POST /cart/quote` | Desglose sin persistir ni tocar stock; `capApplied` + `message` cuando se alcanza el tope |
| Órdenes (HU3) | `POST /orders` (201) | Checkout transaccional; órdenes inmutables |

Salvo el login, todos los endpoints exigen `Authorization: Bearer <token>`.

Errores en formato Problem Details (RFC 9457): 400 validación (con `errors[]`), carrito inválido o JSON mal formado; 401 credenciales o token inválido/expirado; 403; 404 producto o ruta inexistente; 405; 409 (stock con `productId/requested/available`, colisión de `order_number`, integridad); 415; 422 cupón (con `couponCode`); 500 genérico.

## 7. Seguridad

Spring Security con JWT HS256 sin estado. Los usuarios se leen de la tabla `app_users` (contraseñas BCrypt); un usuario con `is_active = 0` no puede iniciar sesión. CORS se configura en `SecurityConfig` a partir de `app.cors.allowed-origins`. Pendiente: autorización por rol y credenciales de BD por variables de entorno. Ver `docs/arquitectura.md` §11.

## 8. Commits sugeridos por incremento

> Plan original de incrementos. Algunos elementos (Testcontainers, Adapter, Observer) no forman parte del código actual. El motor sí usa Strategy + Factory; ver `docs/arquitectura.md` §7.

```
chore(backend): pom, perfiles y configuración base (Boot 3.2.12, MySQL 5.7, sin Flyway)
feat(persistence): entidades JPA, repositorios y pruebas @DataJpaTest con Testcontainers
feat(discounts): motor Strategy + Factory + Adapter con casos a–d del seed
feat(services): catálogo, cupones, carrito, stock y checkout transaccional
feat(api): controllers, DTO, Problem Details, Observer y pruebas de integración
docs: README, arquitectura, IA y colección HTTP
```
