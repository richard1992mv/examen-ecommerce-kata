package com.examen.ecommerce.exception;

/** Única fuente de los mensajes de error de negocio y validación (DRY). */
public final class ErrorMessages {

    public static final String CATEGORY = "Categoría";
    public static final String PRODUCT = "Producto";
    public static final String COUPON = "Cupón";
    public static final String ORDER = "Orden";

    public static final String RESOURCE_NOT_FOUND = "%s con identificador '%s' no encontrado";
    public static final String PRODUCTS_NOT_FOUND = "Productos no encontrados: %s";
    public static final String CATEGORY_HAS_PRODUCTS =
            "La categoría %d tiene productos asociados y no puede eliminarse";
    public static final String PRODUCT_HAS_SALES =
            "El producto %d tiene ventas registradas y no puede eliminarse";
    public static final String COUPON_ALREADY_USED =
            "El cupón %d fue usado en órdenes y no puede eliminarse";
    public static final String DUPLICATE_CATEGORY = "Ya existe una categoría con el nombre '%s'";
    public static final String DUPLICATE_COUPON = "Ya existe un cupón con el código '%s'";
    public static final String INSUFFICIENT_STOCK =
            "Stock insuficiente para el producto %d: solicitada %d, disponible %d";
    public static final String COUPON_NOT_FOUND = "El cupón '%s' no existe";
    public static final String COUPON_INACTIVE = "El cupón '%s' está inactivo";
    public static final String COUPON_OUT_OF_VALIDITY = "El cupón '%s' está fuera de vigencia";
    public static final String CART_EMPTY = "El carrito no puede estar vacío";
    public static final String CART_INVALID_QUANTITY = "La cantidad del producto %d debe ser mayor que 0";
    public static final String CART_INVALID_PRODUCT = "Cada línea del carrito debe indicar un productId válido";
    public static final String CART_DUPLICATED_PRODUCT = "El producto %d está duplicado en el carrito";
    public static final String ORDER_NUMBER_EXHAUSTED =
            "No fue posible generar un número de orden único tras %d intentos";

    public static final String VALIDATION_FAILED = "La solicitud contiene datos inválidos";
    public static final String MALFORMED_REQUEST = "El cuerpo o los parámetros de la solicitud no son válidos";
    public static final String INTEGRITY_VIOLATION = "La operación viola una restricción de integridad de datos";
    public static final String METHOD_NOT_ALLOWED = "Método HTTP no soportado para este recurso";
    public static final String ENDPOINT_NOT_FOUND = "El recurso solicitado no existe";
    public static final String INTERNAL_ERROR = "Ocurrió un error inesperado. Intente nuevamente más tarde";

    public static final String PERCENTAGE_RANGE = "debe estar entre 0 y 100 con máximo 2 decimales";
    public static final String MONEY_POSITIVE = "debe ser mayor que 0 con máximo 2 decimales";
    public static final String DATE_RANGE = "validFrom debe ser menor o igual que validUntil";
    public static final String UNIQUE_PRODUCTS = "no debe contener productId duplicados";

    private ErrorMessages() {
    }
}
