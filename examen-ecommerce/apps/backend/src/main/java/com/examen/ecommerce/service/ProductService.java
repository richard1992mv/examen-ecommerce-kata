package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.response.ProductResponse;
import com.examen.ecommerce.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU1: catálogo de productos pre-configurados. */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAllByOrderByIdAsc().stream().map(ProductResponse::from).toList();
    }
}
