package com.examen.ecommerce.mapper;

import com.examen.ecommerce.dto.request.ProductRequest;
import com.examen.ecommerce.dto.response.ProductResponse;
import com.examen.ecommerce.entity.Category;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.util.MoneyUtils;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(product.getId(), product.getName(), product.getUnitPrice(), product.getStock(),
                category.getId(), category.getName());
    }

    public Product toEntity(ProductRequest request, Category category) {
        return new Product(request.name().trim(), MoneyUtils.round(request.unitPrice()), category, request.stock());
    }

    public void updateEntity(Product product, ProductRequest request, Category category) {
        product.update(request.name().trim(), MoneyUtils.round(request.unitPrice()), category, request.stock());
    }
}
