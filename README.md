# Core E-Commerce con Descuentos Acumulativos

MVP full stack de un módulo de checkout para e-commerce, desarrollado como prueba técnica (KATA). Permite gestionar un carrito de compras y aplicar un motor de descuentos acumulativos en cascada, con validación de stock y persistencia de órdenes.

**Stack:** Java 17 · Spring Boot 3.2.12 · MySQL 5.7.44 · JWT · Angular 22 (standalone + signals) · Bootstrap 5.3 · JUnit 5 / JaCoCo · Vitest.

## Contenido

1. [Reglas de negocio e historias de usuario](#1-reglas-de-negocio-e-historias-de-usuario)
2. [Estructura del repositorio](#2-estructura-del-repositorio)
3. [Requisitos previos](#3-requisitos-previos)
4. [Instalación paso a paso](#4-instalación-paso-a-paso)
5. [Configuración y variables de entorno](#5-configuración-y-variables-de-entorno)
6. [Ejecutar el proyecto](#6-ejecutar-el-proyecto)
7. [Pruebas y cobertura](#7-pruebas-y-cobertura)
8. [Datos de prueba: usuarios, cupones y casos](#8-datos-de-prueba-usuarios-cupones-y-casos)
9. [Solución de problemas](#9-solución-de-problemas)
10. [Documentación](#10-documentación)

---

## 1. Reglas de negocio e historias de usuario

**Reglas de descuento** (cascada multiplicativa, en este orden; redondeo HALF_UP a 2 decimales en cada paso):

1. **Categoría:** 10 % sobre los productos de "Tecnología".
2. **Volumen:** 5 % adicional si el subtotal (tras la regla 1) supera 100.00 USD.
3. **Cupón:** porcentaje del cupón sobre el subtotal tras la regla 2 (`WELCOME2026` = 15 %).
4. **Tope:** el descuento total nunca supera el 35 % del subtotal original.

**Historias de usuario:**

- **HU1:** Gestión del carrito con subtotal en tiempo real.
- **HU2:** Aplicación de cupón y desglose de descuentos.
- **HU3:** Checkout en el backend: valida stock, recalcula descuentos, descuenta inventario y persiste la orden.
- **HU4:** Alerta visual al alcanzar el tope del 35 %.

**Requisitos clave de la KATA:** tipado estricto, arquitectura modular con al menos dos patrones de diseño (Strategy + Factory) y cobertura mínima del 80 % (verificada en el build de backend y frontend).

## 2. Estructura del repositorio

```
examen-ecommerce/
├─ apps/backend/             API REST (Spring Boot): motor de descuentos, checkout y JWT
├─ apps/frontend/            SPA Angular: catálogo, carrito, cupón, desglose y confirmación
├─ docs/
│  ├─ arquitectura.md        Stack, trade-offs, aislamiento del motor, SOLID/DRY y patrones
│  ├─ ia.md                  Uso de IA: agente, prompts y correcciones
│  ├─ migraciones-propuestas.md
│  └─ db_kata_ecommerce/     Scripts SQL (00–04), diccionario de datos y diagrama ER
└─ postman/                  Colección de pruebas de integración HU1–HU4 con JWT
```

## 3. Requisitos previos

| Herramienta | Versión | Para qué | Comprobar |
|---|---|---|---|
| JDK | **17** (también corre en 21) | Backend | `java -version` |
| Maven | **3.9+** | Compilar y probar el backend | `mvn -v` |
| MySQL | **5.7.44** | Base de datos `kata_ecommerce` | `mysql --version` |
| Node.js | **>= 22.22.3** o **>= 24.15** (exigido por Angular CLI 22) | Frontend | `node -v` |
| npm | 10+ (incluido con Node) | Dependencias del frontend | `npm -v` |
| Git | cualquiera | Clonar el repositorio | `git --version` |

Opcional: Postman (colección de integración) y un IDE (IntelliJ IDEA, Spring Tools o VS Code).

> Si `mvn` no está en el PATH, todos los comandos de Maven se pueden ejecutar desde el IDE. Por ejemplo, en Spring Tools / Eclipse: clic derecho sobre el proyecto → **Run As → Maven build…** → *Goals*: `clean verify`.

## 4. Instalación paso a paso

### 4.1 Clonar el repositorio

```bash
git clone https://github.com/richard1992mv/examen-ecommerce-kata.git
cd examen-ecommerce-kata/examen-ecommerce
```

### 4.2 Crear la base de datos

Desde `examen-ecommerce/`, con un usuario de MySQL que pueda crear bases de datos:

```bash
mysql -u root -p < docs/db_kata_ecommerce/00_create_database.sql
mysql -u root -p kata_ecommerce < docs/db_kata_ecommerce/01_schema.sql
mysql -u root -p kata_ecommerce < docs/db_kata_ecommerce/02_seed_data.sql

# Opcional: verificar que los cálculos del seed cuadran
mysql -u root -p kata_ecommerce < docs/db_kata_ecommerce/03_validation_queries.sql
```

| Script | Qué hace |
|---|---|
| `00_create_database.sql` | Crea `kata_ecommerce` (utf8mb4) |
| `01_schema.sql` | Crea las tablas. ⚠️ **Hace `DROP TABLE`**: solo para una instalación nueva o para reiniciar los datos a propósito |
| `02_seed_data.sql` | Carga categorías, 12 productos, 4 cupones, 4 órdenes de referencia y los usuarios de `app_users` |
| `03_validation_queries.sql` | Solo `SELECT`: recalcula la cascada de las órdenes y la compara con lo guardado |
| `04_auth_users.sql` | Solo para una BD creada **antes** de la autenticación JWT: agrega `app_users` sin tocar las demás tablas. En una instalación nueva no hace falta |

La aplicación **no** crea ni modifica el esquema (`ddl-auto=validate`, `spring.sql.init.mode=never`): los scripts son la única fuente de verdad.

### 4.3 Usuario de MySQL para la aplicación

Por defecto, el backend se conecta como `nacadmin` / `admin2015` a `localhost:3306/kata_ecommerce` (valores en `application.properties`). Tienes dos opciones:

**Opción A: crear ese usuario** (desarrollo local):

```sql
CREATE USER 'nacadmin'@'localhost' IDENTIFIED BY 'admin2015';
GRANT SELECT, INSERT, UPDATE, DELETE ON kata_ecommerce.* TO 'nacadmin'@'localhost';
FLUSH PRIVILEGES;
```

**Opción B: usar tu propio usuario** con las variables de entorno `SPRING_DATASOURCE_*` (ver §5).

### 4.4 Instalar las dependencias

```bash
# Backend: descarga las dependencias y compila sin ejecutar pruebas
cd apps/backend
mvn -DskipTests package

# Frontend
cd ../frontend
npm install
```

## 5. Configuración y variables de entorno

### 5.1 Archivos de configuración

| Archivo | Contenido |
|---|---|
| `apps/backend/src/main/resources/application.properties` | Configuración común: puerto, conexión a MySQL, JPA, reglas de descuento, número de orden, OpenAPI y JWT |
| `apps/backend/src/main/resources/application-dev.properties` | Perfil `dev` (el perfil **por defecto**): logs y orígenes CORS permitidos |
| `apps/frontend/src/environments/environment*.ts` | `apiUrl` (`/api/v1`) y moneda por defecto |
| `apps/frontend/proxy.conf.json` | En desarrollo redirige `/api` a `http://localhost:8080` (sin problemas de CORS) |

### 5.2 Variables de entorno del backend

Ninguna es obligatoria para desarrollo local: todas tienen un valor por defecto.

| Variable | Propiedad | Valor por defecto | Uso |
|---|---|---|---|
| `SERVER_PORT` | `server.port` | `8080` | Puerto HTTP de la API |
| `JWT_SECRET` | `app.jwt.secret` | Secreto de desarrollo | Clave HS256 en Base64 (**mínimo 256 bits**). **Obligatorio cambiarla fuera de desarrollo** |
| `JWT_EXPIRATION_MS` | `app.jwt.expiration-ms` | `3600000` (1 hora) | Vigencia del token |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:mysql://localhost:3306/kata_ecommerce?zeroDateTimeBehavior=convertToNull` | URL JDBC de MySQL |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `nacadmin` | Usuario de MySQL |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `admin2015` | Contraseña de MySQL |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | `dev` | Perfil de Spring |
| `APP_CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | `http://localhost:4200,http://localhost:5173,http://localhost:3000` | Orígenes CORS, separados por coma |

`SERVER_PORT`, `JWT_SECRET` y `JWT_EXPIRATION_MS` están declaradas explícitamente en `application.properties`. Las demás funcionan por el *relaxed binding* de Spring Boot: una variable de entorno con el nombre de la propiedad en mayúsculas (puntos y guiones como `_`) tiene prioridad sobre el archivo.

**Ejemplo en PowerShell (Windows):**

```powershell
$env:SPRING_DATASOURCE_USERNAME = "mi_usuario"
$env:SPRING_DATASOURCE_PASSWORD = "mi_clave"
$env:JWT_SECRET = "<secreto-base64-de-32-bytes>"
mvn spring-boot:run
```

**Ejemplo en Bash (Linux / macOS / Git Bash):**

```bash
export SPRING_DATASOURCE_USERNAME=mi_usuario
export SPRING_DATASOURCE_PASSWORD=mi_clave
export JWT_SECRET="$(openssl rand -base64 32)"
mvn spring-boot:run
```

Para generar un secreto JWT en PowerShell:
`[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])`

> Las variables viven solo en la terminal donde se definen. El `.gitignore` ya excluye los archivos `.env` y `application-local.*`, por si prefieres guardarlas localmente sin subirlas al repositorio.

### 5.3 Parámetros de negocio (no son secretos)

Se cambian en `application.properties` sin tocar código. Se validan al arrancar: un valor inválido impide que la aplicación inicie.

| Propiedad | Valor | Regla |
|---|---|---|
| `discount.volume.pct` | `5.00` | % del descuento por volumen |
| `discount.volume.threshold` | `100.00` | Umbral (estrictamente mayor) para aplicar volumen |
| `discount.cap-pct` | `35.00` | Tope del descuento total |
| `order.number.prefix` | `ORD` | Formato `ORD-yyyy-NNNNNN` |
| `order.number.max-attempts` | `3` | Reintentos si dos órdenes generan el mismo número |

Los porcentajes de categoría y de cupón se leen de la base de datos (`categories.discount_pct`, `coupons.discount_pct`).

## 6. Ejecutar el proyecto

Se necesitan **dos terminales**, con MySQL en ejecución.

**Terminal 1 · Backend** (`examen-ecommerce/apps/backend`):

```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080/api/v1`, con perfil `dev` y zona horaria `America/Bogota`.

**Terminal 2 · Frontend** (`examen-ecommerce/apps/frontend`):

```bash
npm start
```

Abre **http://localhost:4200**. El frontend inicia sesión automáticamente con el usuario de demostración y envía el token JWT en cada petición.

| URL | Qué es |
|---|---|
| http://localhost:4200 | Aplicación (carrito y checkout) |
| http://localhost:8080/swagger-ui.html | Swagger UI: documentación y prueba de la API |
| http://localhost:8080/v3/api-docs | Contrato OpenAPI en JSON |

**Probar la API en Swagger:**

1. Ejecuta `POST /api/v1/auth/login` con `{"username": "admin", "password": "admin123"}`.
2. Copia el valor de `accessToken`.
3. Pulsa **Authorize** y pega el token (sin el prefijo `Bearer`).
4. Ejecuta `GET /products`, `POST /cart/quote` o `POST /orders`.

**Build de producción del frontend:**

```bash
npm run build      # salida en apps/frontend/dist/frontend
```

**Endpoints:**

| Método y ruta | HU | Descripción |
|---|---|---|
| `POST /api/v1/auth/login` | — | Público. Devuelve el token JWT |
| `GET /api/v1/products` | HU1 | Catálogo con categoría y stock |
| `POST /api/v1/cart/quote` | HU2 / HU4 | Desglose de descuentos sin persistir ni tocar stock |
| `POST /api/v1/orders` | HU3 | Checkout transaccional (201) |

## 7. Pruebas y cobertura

La cobertura mínima del **80 %** se verifica en el build: **los comandos fallan si no se alcanza**.

### 7.1 Backend (`examen-ecommerce/apps/backend`)

> Requiere **MySQL en ejecución con el seed cargado**: algunas pruebas de integración usan la BD local y revierten sus cambios al terminar (`@Transactional`).

```bash
mvn clean verify                                 # todas las pruebas + reporte + verificación del 80 % (JaCoCo)
mvn test                                         # solo las pruebas
mvn test -Dtest=DiscountCapTest                  # una clase
mvn test -Dtest=CheckoutOrderTest#nombreMetodo   # un método
mvn verify -Djacoco.minimum.coverage=0.99        # demostrar que el umbral bloquea (debe fallar)
```

- Reporte: `apps/backend/target/site/jacoco/index.html`
- Resultado actual: **104 pruebas**; líneas 95.5 %, instrucciones 94.0 %, ramas 84.9 %.

### 7.2 Frontend (`examen-ecommerce/apps/frontend`)

```bash
npm test -- --watch=false     # pruebas unitarias (Vitest), una ejecución
npm test                      # modo observación (se repiten al guardar)
npm run test:coverage         # pruebas + cobertura con verificación del 80 %
```

- Reporte: `apps/frontend/coverage/frontend/index.html`
- Resultado actual: **55 pruebas**; sentencias 99.3 %, ramas 96.6 %, funciones 100 %, líneas 99.0 %.

### 7.3 Pruebas de integración con Postman

1. Importa `examen-ecommerce/postman/KATA-Ecommerce-HU1-HU4-JWT_postman_collection.json`.
2. Con el backend en ejecución, abre el **Collection Runner** y ejecuta la colección **en orden**. La carpeta `AUTH · JWT` guarda el token y las demás peticiones lo heredan.
3. Variables de la colección: `baseUrl` = `http://localhost:8080/api/v1`, `username` = `admin`, `password` = `admin123`.

> ⚠️ Los checkouts de la colección crean órdenes reales y descuentan stock. Con el seed alcanza para unas 8 corridas completas. Para reiniciar los datos, vuelve a ejecutar `01_schema.sql` y `02_seed_data.sql`.

## 8. Datos de prueba: usuarios, cupones y casos

**Usuarios** (tabla `app_users`, contraseñas con BCrypt):

| Usuario | Contraseña | Rol | Estado |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Activo (lo usa el frontend) |
| `cliente` | `cliente123` | USER | Activo |
| `bloqueado` | `bloqueado123` | USER | Inactivo: el login responde 401 |

**Cupones:**

| Código | % | Estado | Resultado esperado |
|---|---|---|---|
| `WELCOME2026` | 15 % | Vigente hasta 2026-12-31 | Se aplica (HU2) |
| `TESTCAP30` | 30 % | Vigente hasta 2026-12-31 | Con un subtotal alto alcanza el tope del 35 % (HU4) |
| `EXPIRED2025` | 20 % | Vencido (2025) | 422 "fuera de vigencia" |
| `INACTIVE10` | 10 % | Inactivo | 422 "inactivo" |

**Casos de referencia** (verificados al centavo por las pruebas):

| Caso | Carrito | Total | Descuento efectivo |
|---|---|---|---|
| (a) | Novela ×2 + Mouse ×1 | 48.00 | 4.00 % |
| (b) | Teclado ×1 + Lámpara ×1 | 96.90 | 11.91 % |
| (c) | Audífonos ×2 + Libro de programación ×1 + `WELCOME2026` | 97.71 | 24.84 % |
| (d) | Monitor + Smartwatch + Balón ×2 + `TESTCAP30` | 208.00 | 35.00 % (alerta HU4) |

## 9. Solución de problemas

| Síntoma | Causa probable y solución |
|---|---|
| `Access denied for user 'nacadmin'` al arrancar | El usuario de MySQL no existe o la clave no coincide: créalo (§4.3) o define `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` (§5.2) |
| `Schema-validation: missing table ...` | Faltan los scripts `01` y `02`, o falta `app_users` en una BD antigua: ejecuta `04_auth_users.sql` |
| `app.jwt.secret debe tener al menos 256 bits` | `JWT_SECRET` es demasiado corto: usa 32 bytes en Base64 (§5.2) |
| Fallan `CheckoutOrderTest`, `CouponBreakdownTest` u otras pruebas con BD | MySQL apagado o sin el seed. Si las pruebas ya descontaron stock vía Postman, recarga `01` y `02` |
| Las pruebas con cupón fallan desde el 2027-01-01 | `WELCOME2026` venció: las pruebas con BD usan la fecha real (pendiente documentado) |
| El frontend muestra "No se pudo conectar con el servidor" | El backend no está en `:8080`, o se abrió el frontend sin `npm start` (sin el proxy) |
| `npm run test:coverage` pide `@vitest/coverage-v8` | Falta ejecutar `npm install` en `apps/frontend` |
| `mvn` no se reconoce como comando | Maven no está en el PATH: instálalo o usa el IDE (§3) |
| Angular CLI rechaza la versión de Node | Actualiza a Node >= 22.22.3 o >= 24.15 |

## 10. Documentación

| Documento | Contenido |
|---|---|
| [`examen-ecommerce/README.md`](examen-ecommerce/README.md) | Detalle del backend: configuración, endpoints, errores (RFC 9457) y seguridad |
| [`examen-ecommerce/apps/frontend/README.md`](examen-ecommerce/apps/frontend/README.md) | Detalle del frontend: autenticación, tipado estricto, pruebas y flujo |
| [`examen-ecommerce/docs/arquitectura.md`](examen-ecommerce/docs/arquitectura.md) | Por qué este stack, trade-offs, aislamiento del motor, SOLID/DRY y patrones |
| [`examen-ecommerce/docs/ia.md`](examen-ecommerce/docs/ia.md) | Agente de IA, prompts completos y las dos correcciones realizadas |
| [`examen-ecommerce/docs/db_kata_ecommerce/base-datos.md`](examen-ecommerce/docs/db_kata_ecommerce/base-datos.md) | Diccionario de datos, diagrama ER y limitaciones de MySQL 5.7 |
| [`examen-ecommerce/docs/migraciones-propuestas.md`](examen-ecommerce/docs/migraciones-propuestas.md) | Migraciones sugeridas (no aplicadas) |
