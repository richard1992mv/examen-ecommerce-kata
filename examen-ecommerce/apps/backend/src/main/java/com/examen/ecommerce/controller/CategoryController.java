package com.examen.ecommerce.controller;

import com.examen.ecommerce.dto.request.CategoryRequest;
import com.examen.ecommerce.dto.response.CategoryResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categorías")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Listar categorías (paginado)")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.CATEGORY_PAGE)))
    public PageResponse<CategoryResponse> findAll(@PageableDefault(sort = "id") Pageable pageable) {
        return categoryService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una categoría")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.CATEGORY_RESPONSE)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    public CategoryResponse findById(@PathVariable Integer id) {
        return categoryService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear categoría", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(ApiExamples.CATEGORY_REQUEST))))
    @ApiResponse(responseCode = "201", content = @Content(examples = @ExampleObject(ApiExamples.CATEGORY_RESPONSE)))
    @ApiResponse(responseCode = "400", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_VALIDATION)))
    @ApiResponse(responseCode = "409", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_CONFLICT)))
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return Responses.created(created.id(), created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(ApiExamples.CATEGORY_REQUEST))))
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.CATEGORY_RESPONSE)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    @ApiResponse(responseCode = "409", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_CONFLICT)))
    public CategoryResponse update(@PathVariable Integer id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría (409 si tiene productos)")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    @ApiResponse(responseCode = "409", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_CONFLICT)))
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return Responses.noContent();
    }
}
