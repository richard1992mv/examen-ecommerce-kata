package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.dto.response.OrderSummaryResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;

/** HU3 (lectura): consulta de órdenes, separada de la escritura (ISP). */
public interface OrderQueryService {

    PageResponse<OrderSummaryResponse> findAll(LocalDateTime from, LocalDateTime to, String couponCode,
                                               Pageable pageable);

    OrderResponse findById(Integer id);

    OrderResponse findByOrderNumber(String orderNumber);
}
