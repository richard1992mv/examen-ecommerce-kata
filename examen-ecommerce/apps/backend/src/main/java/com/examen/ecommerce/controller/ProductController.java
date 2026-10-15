package com.examen.ecommerce.controller;

import com.examen.ecommerce.dto.request.ProductRequest;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.dto.response.ProductResponse;
import com.examen.ecommerce.service.ProductService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HU1: catálogo de productos. */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Productos (HU1)")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Listar productos con filtros opcionales por categoría y nombre")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.PRODUCT_PAGE)))
    public PageResponse<ProductResponse> findAll(@RequestParam(required = false) Integer categoryId,
                                                 @RequestParam(required = false) String name,
                                                 @PageableDefault(sort = "id") Pageable pageable) {
        return productService.findAll(categoryId, name, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.PRODUCT_RESPONSE)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    public ProductResponse findById(@PathVariable Integer id) {
        return productService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear producto (unitPrice > 0, stock ≥ 0)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(ApiExamples.PRODUCT_REQUEST))))
    @ApiResponse(responseCode = "201", content = @Content(examples = @ExampleObject(ApiExamples.PRODUCT_RESPONSE)))
    @ApiResponse(responseCode = "400", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_VALIDATION)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return Responses.created(created.id(), created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(ApiExamples.PRODUCT_REQUEST))))
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.PRODUCT_RESPONSE)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    public ProductResponse update(@PathVariable Integer id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto (409 si tiene ventas)")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "409", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_CONFLICT)))
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        productService.delete(id);
        return Responses.noContent();
    }
}
