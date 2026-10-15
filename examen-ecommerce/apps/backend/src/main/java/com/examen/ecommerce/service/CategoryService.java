package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CategoryRequest;
import com.examen.ecommerce.dto.response.CategoryResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    PageResponse<CategoryResponse> findAll(Pageable pageable);

    CategoryResponse findById(Integer id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Integer id, CategoryRequest request);

    void delete(Integer id);
}
