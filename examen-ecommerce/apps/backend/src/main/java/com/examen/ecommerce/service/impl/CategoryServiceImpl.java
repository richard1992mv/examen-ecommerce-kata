package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.dto.request.CategoryRequest;
import com.examen.ecommerce.dto.response.CategoryResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.entity.Category;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ResourceConflictException;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.mapper.CategoryMapper;
import com.examen.ecommerce.repository.CategoryRepository;
import com.examen.ecommerce.repository.ProductRepository;
import com.examen.ecommerce.service.CategoryService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper mapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository,
                               CategoryMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Override
    public PageResponse<CategoryResponse> findAll(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Override
    public CategoryResponse findById(Integer id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ResourceConflictException(ErrorMessages.DUPLICATE_CATEGORY.formatted(request.name()));
        }
        return mapper.toResponse(categoryRepository.save(mapper.toEntity(request)));
    }

    @Override
    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = getOrThrow(id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ResourceConflictException(ErrorMessages.DUPLICATE_CATEGORY.formatted(request.name()));
        }
        mapper.updateEntity(category, request);
        return mapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Category category = getOrThrow(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ResourceConflictException(ErrorMessages.CATEGORY_HAS_PRODUCTS.formatted(id));
        }
        categoryRepository.delete(category);
    }

    private Category getOrThrow(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.CATEGORY, id));
    }
}
