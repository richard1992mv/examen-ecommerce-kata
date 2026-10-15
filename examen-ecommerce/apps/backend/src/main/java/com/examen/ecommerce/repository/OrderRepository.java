package com.examen.ecommerce.repository;

import com.examen.ecommerce.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Integer id);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsByOrderNumber(String orderNumber);

    /** Todas las órdenes con sus ítems en UNA consulta (JOIN FETCH, sin N+1). */
    @Query("select distinct o from PurchaseOrder o left join fetch o.items order by o.id")
    List<Order> findAllWithItems();

    /** Último número del año: el formato de ancho fijo permite usar max() lexicográfico. */
    @Query("select max(o.orderNumber) from PurchaseOrder o where o.orderNumber like concat(:prefix, '%')")
    Optional<String> findLastOrderNumber(@Param("prefix") String prefix);

    boolean existsByCouponId(Integer couponId);
}
