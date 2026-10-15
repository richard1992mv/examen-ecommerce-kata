package com.examen.ecommerce.controller;

/** Ejemplos OpenAPI (request/response) tomados de 02_seed_data.sql. */
final class ApiExamples {

    static final String CATEGORY_REQUEST = """
            {"name": "Tecnología", "discountPct": 10.00}""";
    static final String CATEGORY_RESPONSE = """
            {"id": 1, "name": "Tecnología", "discountPct": 10.00}""";
    static final String CATEGORY_PAGE = """
            {"content": [{"id": 1, "name": "Tecnología", "discountPct": 10.00},
                         {"id": 2, "name": "Hogar", "discountPct": 0.00}],
             "page": 0, "size": 20, "totalElements": 4, "totalPages": 1}""";

    static final String PRODUCT_REQUEST = """
            {"name": "Teclado mecánico", "unitPrice": 80.00, "categoryId": 1, "stock": 15}""";
    static final String PRODUCT_RESPONSE = """
            {"id": 3, "name": "Teclado mecánico", "unitPrice": 80.00, "stock": 15,
             "categoryId": 1, "categoryName": "Tecnología"}""";
    static final String PRODUCT_PAGE = """
            {"content": [{"id": 3, "name": "Teclado mecánico", "unitPrice": 80.00, "stock": 15,
                          "categoryId": 1, "categoryName": "Tecnología"}],
             "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}""";

    static final String COUPON_REQUEST = """
            {"code": "BLACKFRIDAY", "discountPct": 20.00, "validFrom": "2026-11-27T00:00:00",
             "validUntil": "2026-11-30T23:59:59", "active": true}""";
    static final String COUPON_RESPONSE = """
            {"id": 1, "code": "WELCOME2026", "discountPct": 15.00, "validFrom": "2026-01-01T00:00:00",
             "validUntil": "2026-12-31T23:59:59", "active": true}""";
    static final String COUPON_PAGE = """
            {"content": [{"id": 1, "code": "WELCOME2026", "discountPct": 15.00,
                          "validFrom": "2026-01-01T00:00:00", "validUntil": "2026-12-31T23:59:59",
                          "active": true}],
             "page": 0, "size": 20, "totalElements": 4, "totalPages": 1}""";

    static final String CART_REQUEST_CAP = """
            {"items": [{"productId": 4, "quantity": 1}, {"productId": 12, "quantity": 1},
                       {"productId": 10, "quantity": 2}],
             "couponCode": "TESTCAP30"}""";
    static final String CART_REQUEST_COUPON = """
            {"items": [{"productId": 1, "quantity": 2}, {"productId": 9, "quantity": 1}],
             "couponCode": "WELCOME2026"}""";
    static final String QUOTE_RESPONSE_CAP = """
            {"subtotalOriginal": 320.00, "categoryDiscount": 27.00, "volumeDiscount": 14.65,
             "couponDiscount": 83.51, "capAdjustment": 13.16, "discountTotal": 112.00,
             "effectiveDiscountPct": 35.00, "discountCapPct": 35.00, "capApplied": true,
             "message": "¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)",
             "total": 208.00, "currency": "USD", "couponCode": "TESTCAP30",
             "items": [
               {"productId": 4, "productName": "Monitor 24 pulgadas", "categoryName": "Tecnología",
                "unitPrice": 150.00, "quantity": 1, "lineSubtotal": 150.00, "categoryDiscount": 15.00,
                "lineTotal": 135.00},
               {"productId": 12, "productName": "Smartwatch", "categoryName": "Tecnología",
                "unitPrice": 120.00, "quantity": 1, "lineSubtotal": 120.00, "categoryDiscount": 12.00,
                "lineTotal": 108.00},
               {"productId": 10, "productName": "Balón de fútbol", "categoryName": "Deportes",
                "unitPrice": 25.00, "quantity": 2, "lineSubtotal": 50.00, "categoryDiscount": 0.00,
                "lineTotal": 50.00}]}""";
    static final String ORDER_RESPONSE = """
            {"id": 5, "orderNumber": "ORD-2026-000005", "createdAt": "2026-10-15T12:00:00",
             "couponCode": "WELCOME2026", "subtotalOriginal": 130.00, "categoryDiscount": 9.00,
             "volumeDiscount": 6.05, "couponDiscount": 17.24, "capAdjustment": 0.00, "discountTotal": 32.29,
             "effectiveDiscountPct": 24.84, "discountCapPct": 35.00, "capApplied": false, "message": null,
             "total": 97.71, "currency": "USD",
             "items": [
               {"id": 10, "productId": 1, "productName": "Audífonos Bluetooth", "categoryName": "Tecnología",
                "unitPrice": 45.00, "quantity": 2, "lineSubtotal": 90.00, "categoryDiscount": 9.00,
                "lineTotal": 81.00},
               {"id": 11, "productId": 9, "productName": "Libro de programación", "categoryName": "Libros",
                "unitPrice": 40.00, "quantity": 1, "lineSubtotal": 40.00, "categoryDiscount": 0.00,
                "lineTotal": 40.00}]}""";
    static final String ORDER_PAGE = """
            {"content": [{"id": 4, "orderNumber": "ORD-2026-000004", "createdAt": "2026-10-08T19:30:00",
                          "couponCode": "TESTCAP30", "subtotalOriginal": 320.00, "discountTotal": 112.00,
                          "total": 208.00, "capApplied": true, "currency": "USD"}],
             "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}""";

    static final String PROBLEM_VALIDATION = """
            {"type": "urn:problem-type:validation-error", "title": "Bad Request", "status": 400,
             "detail": "La solicitud contiene datos inválidos", "instance": "/api/v1/cart/quote",
             "errors": [{"field": "items[0].quantity", "message": "debe ser mayor que 0"}]}""";
    static final String PROBLEM_NOT_FOUND = """
            {"type": "urn:problem-type:resource-not-found", "title": "Not Found", "status": 404,
             "detail": "Productos no encontrados: [999]", "instance": "/api/v1/cart/quote"}""";
    static final String PROBLEM_STOCK = """
            {"type": "urn:problem-type:insufficient-stock", "title": "Conflict", "status": 409,
             "detail": "Stock insuficiente para el producto 7: solicitada 3, disponible 2",
             "instance": "/api/v1/orders", "productId": 7, "requested": 3, "available": 2}""";
    static final String PROBLEM_CONFLICT = """
            {"type": "urn:problem-type:resource-conflict", "title": "Conflict", "status": 409,
             "detail": "La categoría 1 tiene productos asociados y no puede eliminarse",
             "instance": "/api/v1/categories/1"}""";
    static final String PROBLEM_COUPON = """
            {"type": "urn:problem-type:invalid-coupon", "title": "Unprocessable Entity", "status": 422,
             "detail": "El cupón 'EXPIRED2025' está fuera de vigencia", "instance": "/api/v1/cart/quote",
             "couponCode": "EXPIRED2025"}""";

    private ApiExamples() {
    }
}
