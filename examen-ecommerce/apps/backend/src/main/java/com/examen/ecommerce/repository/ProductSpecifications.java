package com.examen.ecommerce.repository;

import com.examen.ecommerce.entity.Product;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionales de GET /products (un filtro nulo no restringe). */
public final class ProductSpecifications {

    private static final String WILDCARD = "%";

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(Integer categoryId, String name) {
        return Specification.where(hasCategory(categoryId)).and(nameContains(name));
    }

    static Specification<Product> hasCategory(Integer categoryId) {
        return (root, query, cb) -> categoryId == null
                ? null
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Product> nameContains(String name) {
        return (root, query, cb) -> name == null || name.isBlank()
                ? null
                : cb.like(cb.lower(root.get("name")), WILDCARD + name.trim().toLowerCase(Locale.ROOT) + WILDCARD);
    }
}
