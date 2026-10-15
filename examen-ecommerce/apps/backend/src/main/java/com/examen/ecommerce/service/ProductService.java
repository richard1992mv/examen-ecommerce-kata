package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.ProductRequest;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    PageResponse<ProductResponse> findAll(Integer categoryId, String name, Pageable pageable);

    ProductResponse findById(Integer id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Integer id, ProductRequest request);

    void delete(Integer id);
}
