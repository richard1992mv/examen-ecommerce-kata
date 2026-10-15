package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.dto.request.ProductRequest;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.dto.response.ProductResponse;
import com.examen.ecommerce.entity.Category;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ResourceConflictException;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.mapper.ProductMapper;
import com.examen.ecommerce.repository.CategoryRepository;
import com.examen.ecommerce.repository.OrderItemRepository;
import com.examen.ecommerce.repository.ProductRepository;
import com.examen.ecommerce.repository.ProductSpecifications;
import com.examen.ecommerce.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper mapper;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository,
                              OrderItemRepository orderItemRepository, ProductMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
    }

    @Override
    public PageResponse<ProductResponse> findAll(Integer categoryId, String name, Pageable pageable) {
        return PageResponse.from(productRepository
                .findAll(ProductSpecifications.withFilters(categoryId, name), pageable)
                .map(mapper::toResponse));
    }

    @Override
    public ProductResponse findById(Integer id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = getCategoryOrThrow(request.categoryId());
        return mapper.toResponse(productRepository.save(mapper.toEntity(request, category)));
    }

    @Override
    @Transactional
    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = getOrThrow(id);
        mapper.updateEntity(product, request, getCategoryOrThrow(request.categoryId()));
        return mapper.toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Product product = getOrThrow(id);
        if (orderItemRepository.existsByProductId(id)) {
            throw new ResourceConflictException(ErrorMessages.PRODUCT_HAS_SALES.formatted(id));
        }
        productRepository.delete(product);
    }

    private Product getOrThrow(Integer id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PRODUCT, id));
    }

    private Category getCategoryOrThrow(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.CATEGORY, categoryId));
    }
}
