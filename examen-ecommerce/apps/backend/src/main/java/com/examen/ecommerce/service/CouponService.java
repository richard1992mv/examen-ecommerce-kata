package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CouponRequest;
import com.examen.ecommerce.dto.response.CouponResponse;
import com.examen.ecommerce.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CouponService {

    PageResponse<CouponResponse> findAll(Pageable pageable);

    CouponResponse findByCode(String code);

    CouponResponse create(CouponRequest request);

    CouponResponse update(Integer id, CouponRequest request);

    void delete(Integer id);
}
