package com.examen.ecommerce.controller;

import com.examen.ecommerce.dto.request.CouponRequest;
import com.examen.ecommerce.dto.response.CouponResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.service.CouponService;
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
@RequestMapping("/api/v1/coupons")
@Tag(name = "Cupones")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    @Operation(summary = "Listar cupones (paginado)")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.COUPON_PAGE)))
    public PageResponse<CouponResponse> findAll(@PageableDefault(sort = "id") Pageable pageable) {
        return couponService.findAll(pageable);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Obtener un cupón por código")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.COUPON_RESPONSE)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    public CouponResponse findByCode(@PathVariable String code) {
        return couponService.findByCode(code);
    }

    @PostMapping
    @Operation(summary = "Crear cupón (validFrom ≤ validUntil, 0 ≤ discountPct ≤ 100)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(ApiExamples.COUPON_REQUEST))))
    @ApiResponse(responseCode = "201", content = @Content(examples = @ExampleObject(ApiExamples.COUPON_RESPONSE)))
    @ApiResponse(responseCode = "400", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_VALIDATION)))
    @ApiResponse(responseCode = "409", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_CONFLICT)))
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        CouponResponse created = couponService.create(request);
        return Responses.created(created.id(), created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cupón", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(ApiExamples.COUPON_REQUEST))))
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.COUPON_RESPONSE)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    public CouponResponse update(@PathVariable Integer id, @Valid @RequestBody CouponRequest request) {
        return couponService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cupón (409 si fue usado)")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "409", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_CONFLICT)))
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        couponService.delete(id);
        return Responses.noContent();
    }
}
