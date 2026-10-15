package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.dto.response.OrderSummaryResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.mapper.OrderMapper;
import com.examen.ecommerce.repository.OrderRepository;
import com.examen.ecommerce.repository.OrderSpecifications;
import com.examen.ecommerce.service.OrderQueryService;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper mapper;

    public OrderQueryServiceImpl(OrderRepository orderRepository, OrderMapper mapper) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
    }

    @Override
    public PageResponse<OrderSummaryResponse> findAll(LocalDateTime from, LocalDateTime to, String couponCode,
                                                      Pageable pageable) {
        return PageResponse.from(orderRepository
                .findAll(OrderSpecifications.withFilters(from, to, couponCode), pageable)
                .map(mapper::toSummary));
    }

    @Override
    public OrderResponse findById(Integer id) {
        return orderRepository.findWithItemsById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.ORDER, id));
    }

    @Override
    public OrderResponse findByOrderNumber(String orderNumber) {
        return orderRepository.findWithItemsByOrderNumber(orderNumber)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.ORDER, orderNumber));
    }
}
