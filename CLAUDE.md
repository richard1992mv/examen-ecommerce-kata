# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Technical-test KATA: full-stack e-commerce checkout with a cascading cumulative discount engine. All code comments, docs, error messages and test `@DisplayName`s are in **Spanish**; keep that convention.

Discount rules, applied in this order (multiplicative cascade, `HALF_UP` rounding to 2 decimals at every step):
1. **Category:** per line, `% of its category` (Tecnología = 10 %), read from `categories.discount_pct`.
2. **Volume:** +5 % on subtotal after rule 1 if it is **> 100.00** USD.
3. **Coupon:** +coupon % on subtotal after rule 2 (`WELCOME2026` = 15 %, expires 2026-12-31).
4. **Cap:** total discount never exceeds 35 % of the original subtotal; when hit, `capApplied=true` and an `adjustment` line is reported (HU4 alert in the UI).

Volume %, threshold and cap come from `application.properties` (`discount.*`), never hardcoded. Requirements: strict typing, ≥ 2 design patterns, ≥ 80 % unit test coverage.

## Layout

Everything lives under `examen-ecommerce/`:
- `apps/backend/` – Spring Boot 3.2.12, Java 17, MySQL 5.7 (database `kata_ecommerce`), JWT (jjwt 0.12.6).
- `apps/frontend/` – Angular 22 standalone + signals, Bootstrap 5.3 (CSS only), Font Awesome 7, Vitest.
- `docs/db_kata_ecommerce/` – the SQL scripts (`00_create_database` → `01_schema` → `02_seed_data` → `03_validation_queries` → `04_auth_users`), ER diagram and `base-datos.md` data dictionary.
- `docs/arquitectura.md`, `docs/ia.md`, `docs/plan.md` – design notes and AI-usage log.
- `postman/` – integration collection for HU1–HU4 with JWT.

## Commands

Backend (run from `examen-ecommerce/apps/backend`):
```bash
mvn clean verify                                   # compile + all tests + JaCoCo check (≥ 80 % lines/instructions/branches)
mvn test -Dtest=DiscountCapTest                    # single test class
mvn test -Dtest=CheckoutOrderTest#methodName       # single test method
mvn spring-boot:run                                # starts on :8080 with profile dev
```
Swagger UI: `http://localhost:8080/swagger-ui.html`.

Frontend (run from `examen-ecommerce/apps/frontend`, Node ≥ 22.22.3):
```bash
npm install
npm start                          # ng serve on :4200; proxy.conf.json forwards /api → localhost:8080
npm run build
npm test -- --watch=false          # Vitest
npm run test:coverage              # Vitest + v8 coverage; fails below 80 % (thresholds in angular.json)
npx prettier --write src           # formatting (.prettierrc)
```

Database setup (first time only — `01_schema.sql` does `DROP TABLE`):
```bash
mysql -u root -p < docs/db_kata_ecommerce/00_create_database.sql
mysql -u root -p kata_ecommerce < docs/db_kata_ecommerce/01_schema.sql
mysql -u root -p kata_ecommerce < docs/db_kata_ecommerce/02_seed_data.sql
```
`04_auth_users.sql` is only a migration for a database created before JWT auth (it adds `app_users` without touching other tables); fresh installs already get `app_users` from `01`/`02`.

## Backend architecture

Layered MVC in `com.examen.ecommerce`: `controller` → `service` → `repository`/`entity`, with `dto` (records + `@UniqueProductIds` validator), `mapper/CheckoutMapper`, `exception/GlobalExceptionHandler` (RFC 9457 `ProblemDetail`, always built via `ProblemDetails.of`, also by `AuthController` and the 401/403 handlers; all messages, including security ones, centralized in `ErrorMessages`), `config` (`@ConfigurationProperties` records), `security`.

**Single pricing path for quote and checkout.** `PricingService.price()` is the only place a cart is priced: validate cart → load all products in one query (`findAllWithCategoryByIdIn`) → validate coupon → assert stock → `DiscountCalculator.calculate()`. Prices, percentages and stock always come from the DB; amounts sent by the client are ignored.
- `POST /api/v1/cart/quote` (HU2/HU4) → `PricingService.quote()`, read-only, no persistence.
- `POST /api/v1/orders` (HU3) → `OrderService.checkout()`: each attempt runs in its own `TransactionTemplate` transaction (price → atomic stock `UPDATE` via `StockService.decrement` → generate `ORD-yyyy-NNNNNN` → `saveAndFlush`). A collision on the `uk_orders_order_number` UNIQUE constraint triggers a bounded retry (`order.number.max-attempts`). Stock decrement is all-or-nothing.
- Orders/order_items store snapshots (product name, category, unit price, coupon) and are immutable.

`DiscountCalculator` (`service/discount/`) is pure computation over `DiscountLine` → `DiscountResult`; all money math goes through `util/MoneyUtils`. It implements the kata's two required patterns: **Strategy** (`DiscountRule` interface with package-private `CategoryDiscountRule`, `VolumeDiscountRule`, `CouponDiscountRule`; `DiscountType` maps each amount to its breakdown field) and **Factory** (`DiscountRuleFactory.createRules(couponPct)`, the only place rules are instantiated and their cascade order is fixed). The calculator loops over the rules passing the running subtotal, then applies the cap itself (the cap is not a rule). The factory is a `@Component` injected into `DiscountCalculator(DiscountRuleFactory, DiscountProperties)`; unit tests build it with `new DiscountRuleFactory(properties)`. Current time always comes from the injected `Clock` bean (`config/TimeConfig`, `Clock.systemDefaultZone()`), including `JwtService` token issue/validation; never call `now()` without it. A `@WebMvcTest` that loads `JwtService` must `@Import(TimeConfig.class)`.

**Security:** stateless JWT HS256. `POST /api/v1/auth/login` and Swagger paths are public; everything else requires `Authorization: Bearer`. Users are loaded from MySQL table `app_users` (BCrypt) via `DatabaseUserDetailsService`. 401/403 are rendered as ProblemDetail by `JwtAuthenticationEntryPoint` / `JwtAccessDeniedHandler`. CORS origins come from `app.cors.allowed-origins`.

**Schema is owned by SQL scripts, not Hibernate:** `ddl-auto=validate`, `spring.sql.init.mode=never`, no Flyway. Entity changes must be mirrored in `docs/db_kata_ecommerce/01_schema.sql` (and the data dictionary). JVM/Hibernate time zone is `America/Bogota` (set via `app.time-zone` in the pom for surefire and spring-boot:run).

## Tests

- Pure unit tests: `service/discount/*Test` (calculator and `DiscountRuleFactoryTest`), `CouponValidatorTest`, `DatabaseUserDetailsServiceTest`.
- `SecurityIntegrationTest` is a `@WebMvcTest` with mocked repositories (no DB).
- `CheckoutOrderTest`, `CouponBreakdownTest`, `ProductCatalogTest`, `DiscountCapAlertTest`, `AuthLoginDatabaseTest` are `@SpringBootTest` with **`@ActiveProfiles("dev")`** — they hit the **local MySQL dev database** with the seed loaded, and are `@Transactional` so they roll back. MySQL must be running for `mvn verify` to pass. Expected amounts in these tests are tied to the seed cases (e.g. case (c) `130.00 − 32.29 = 97.71`, case (d) with coupon `TESTCAP30` hits the cap).
- Pure unit tests also cover `CartValidatorTest` and `IdentifiedEntityTest`; `OrderServiceRetryTest` uses Mockito for the `order_number` collision retry.
- Coverage is a build gate on both sides (≥ 80 %): JaCoCo `check` in `mvn verify` (excludes only `EcommerceApplication`), and `coverageThresholds` in `angular.json` over all of `src/app` (excludes specs, `testing/`, `models/`, `icons.ts`, `app.config.ts`). New code needs tests or the build goes red.
- Frontend: a spec per service, interceptor and component; shared data in `src/app/testing/fixtures.ts`; HTTP via `HttpTestingController`.

## Frontend architecture

No router; `app.ts` composes the components (`product-list`, `cart`, `coupon-form`, `discount-summary`, `cap-alert`, `order-confirmation`). `CartService` holds cart state in signals and re-requests `/cart/quote` automatically when items or coupon change — the backend quote is the source of truth for discounts, the frontend does not compute them. `auth.interceptor.ts` logs in transparently with fixed demo credentials (a seed user from `app_users`), caches the JWT in memory, and retries once on 401; the login call uses `HttpBackend` to bypass the interceptor. Icons must be exported from `src/app/icons.ts` before use. Strict typing is a kata requirement: no `any`/`$any` (read inputs via typed template refs, not `event.target` casts), API contracts live in `src/app/models` mirroring the backend DTOs, and untyped errors are `unknown` narrowed with `instanceof`/type guards. Run `npx prettier --write src` after frontend edits. API base URL is `environment.apiUrl` (`/api/v1`, via the dev proxy).

## Known gaps vs. kata requirements

Documented as pending in `docs/arquitectura.md` §7/§11 and `examen-ecommerce/README.md` §4–§5: the Adapter/Observer classes were removed in commit `9a8c8a3` and are not coming back (Strategy/Factory were later reintroduced in minimal form, see above), the DB tests still use the real clock (so they depend on `WELCOME2026` not having expired), Testcontainers is declared in the pom but unused, DB credentials are hardcoded in `application.properties`, and no route enforces roles. `docs/plan.md` is the original pre-coding plan and does not describe the current code. When docs and code disagree, the code is authoritative.
