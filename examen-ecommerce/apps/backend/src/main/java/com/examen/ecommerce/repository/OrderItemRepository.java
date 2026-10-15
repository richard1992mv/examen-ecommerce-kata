package com.examen.ecommerce.repository;

import com.examen.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    boolean existsByProductId(Integer productId);
}
