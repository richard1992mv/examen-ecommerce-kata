package com.examen.ecommerce.dto.response;

import com.examen.ecommerce.entity.Product;
import java.math.BigDecimal;

/** HU1: producto pre-configurado (id, nombre, precio unitario, categoría y stock). */
public record ProductResponse(
        Integer id,
        String name,
        BigDecimal unitPrice,
        Integer stock,
        Integer categoryId,
        String categoryName) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getUnitPrice(), product.getStock(),
                product.getCategory().getId(), product.getCategory().getName());
    }
}
