package com.examen.ecommerce.controller;

import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.dto.response.OrderSummaryResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.service.OrderQueryService;
import com.examen.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HU3: checkout y consulta de órdenes (inmutables: sin PUT ni DELETE). */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Órdenes (HU3)")
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    public OrderController(OrderService orderService, OrderQueryService orderQueryService) {
        this.orderService = orderService;
        this.orderQueryService = orderQueryService;
    }

    @PostMapping
    @Operation(summary = "Confirmar compra (transaccional, descuenta stock)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                @ExampleObject(name = "Con cupón (caso c)", value = ApiExamples.CART_REQUEST_COUPON),
                @ExampleObject(name = "Con tope (caso d)", value = ApiExamples.CART_REQUEST_CAP)})))
    @ApiResponse(responseCode = "201", content = @Content(examples = @ExampleObject(ApiExamples.ORDER_RESPONSE)))
    @ApiResponse(responseCode = "400", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_VALIDATION)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    @ApiResponse(responseCode = "409", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_STOCK)))
    @ApiResponse(responseCode = "422", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_COUPON)))
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CartRequest request) {
        OrderResponse created = orderService.checkout(request);
        return Responses.created(created.id(), created);
    }

    @GetMapping
    @Operation(summary = "Listar órdenes con filtros de fecha (ISO, inclusive) y cupón")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.ORDER_PAGE)))
    public PageResponse<OrderSummaryResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String couponCode,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderQueryService.findAll(from, to, couponCode, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener orden por id")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.ORDER_RESPONSE)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    public OrderResponse findById(@PathVariable Integer id) {
        return orderQueryService.findById(id);
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Obtener orden por número (ORD-2026-000001)")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.ORDER_RESPONSE)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    public OrderResponse findByOrderNumber(@PathVariable String orderNumber) {
        return orderQueryService.findByOrderNumber(orderNumber);
    }
}
