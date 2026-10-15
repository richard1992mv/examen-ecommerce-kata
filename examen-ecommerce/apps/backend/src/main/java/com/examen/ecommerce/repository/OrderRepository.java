package com.examen.ecommerce.repository;

import com.examen.ecommerce.entity.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    /** Último número del año: el formato de ancho fijo permite usar max() lexicográfico. */
    @Query("select max(o.orderNumber) from PurchaseOrder o where o.orderNumber like concat(:prefix, '%')")
    Optional<String> findLastOrderNumber(@Param("prefix") String prefix);
}
