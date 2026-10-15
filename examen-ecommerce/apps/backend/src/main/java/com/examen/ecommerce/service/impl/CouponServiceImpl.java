package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.dto.request.CouponRequest;
import com.examen.ecommerce.dto.response.CouponResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ResourceConflictException;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.mapper.CouponMapper;
import com.examen.ecommerce.repository.CouponRepository;
import com.examen.ecommerce.repository.OrderRepository;
import com.examen.ecommerce.service.CouponService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final CouponMapper mapper;

    public CouponServiceImpl(CouponRepository couponRepository, OrderRepository orderRepository,
                             CouponMapper mapper) {
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.mapper = mapper;
    }

    @Override
    public PageResponse<CouponResponse> findAll(Pageable pageable) {
        return PageResponse.from(couponRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Override
    public CouponResponse findByCode(String code) {
        return couponRepository.findByCode(code.trim())
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.COUPON, code));
    }

    @Override
    @Transactional
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCode(request.code().trim())) {
            throw new ResourceConflictException(ErrorMessages.DUPLICATE_COUPON.formatted(request.code()));
        }
        return mapper.toResponse(couponRepository.save(mapper.toEntity(request)));
    }

    @Override
    @Transactional
    public CouponResponse update(Integer id, CouponRequest request) {
        Coupon coupon = getOrThrow(id);
        if (couponRepository.existsByCodeAndIdNot(request.code().trim(), id)) {
            throw new ResourceConflictException(ErrorMessages.DUPLICATE_COUPON.formatted(request.code()));
        }
        mapper.updateEntity(coupon, request);
        return mapper.toResponse(coupon);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Coupon coupon = getOrThrow(id);
        if (orderRepository.existsByCouponId(id)) {
            throw new ResourceConflictException(ErrorMessages.COUPON_ALREADY_USED.formatted(id));
        }
        couponRepository.delete(coupon);
    }

    private Coupon getOrThrow(Integer id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.COUPON, id));
    }
}
