## Core E-Commerce con Descuentos Acumulativos

MVP Full Stack de un módulo de checkout para e-commerce, desarrollado como prueba técnica (KATA). Permite gestionar un carrito de compras y aplicar un motor de descuentos acumulativos en cascada, con validación de stock y persistencia de órdenes.

### Reglas de descuento (en orden)
1. **Categoría:** 10 % sobre los productos de "Tecnología".
2. **Volumen:** 5 % adicional si el subtotal (tras la regla 1) supera 100 USD.
3. **Cupón:** 15 % adicional con el código `WELCOME2026`.
4. **Tope:** el descuento total nunca supera el 35 % del subtotal original.

### Historias de usuario
- **HU1:** Gestión del carrito con subtotal en tiempo real.
- **HU2:** Aplicación de cupón y desglose de descuentos.
- **HU3:** Checkout en backend: valida stock, recalcula descuentos, descuenta inventario y persiste la orden.
- **HU4:** Alerta visual al alcanzar el tope del 35 %.

### Estructura
Todo el proyecto está en `examen-ecommerce/`:
- `apps/backend/` – API REST (Spring Boot 3.2, MySQL 5.7) con el motor de descuentos y autenticación JWT.
- `apps/frontend/` – Interfaz Angular del carrito y checkout.
- `docs/arquitectura.md` – Decisiones de diseño, trade-offs y patrones.
- `docs/ia.md` – Gobernanza y uso de IA (prompts, agentes, correcciones).
- `docs/db_kata_ecommerce/` – Scripts SQL, diccionario de datos y diagrama ER.
- `postman/` – Colección de pruebas de integración (HU1–HU4 con JWT).

Instrucciones de instalación y ejecución en `examen-ecommerce/README.md` y `examen-ecommerce/apps/frontend/README.md`.

### Requisitos clave
Tipado estricto, arquitectura modular con al menos dos patrones de diseño y cobertura mínima de pruebas unitarias del 80 %.
