package com.examen.ecommerce.mapper;

import com.examen.ecommerce.dto.request.CategoryRequest;
import com.examen.ecommerce.dto.response.CategoryResponse;
import com.examen.ecommerce.entity.Category;
import com.examen.ecommerce.util.MoneyUtils;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDiscountPct());
    }

    public Category toEntity(CategoryRequest request) {
        return new Category(request.name().trim(), MoneyUtils.round(request.discountPct()));
    }

    public void updateEntity(Category category, CategoryRequest request) {
        category.update(request.name().trim(), MoneyUtils.round(request.discountPct()));
    }
}
